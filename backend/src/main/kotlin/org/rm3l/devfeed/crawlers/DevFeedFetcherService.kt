package org.rm3l.devfeed.crawlers

import org.rm3l.devfeed.dal.DevFeedDao
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService

@Service
class DevFeedFetcherService(private val dao: DevFeedDao,
                            private val crawlers: Collection<DevFeedCrawler>? = null):
        ApplicationListener<ApplicationReadyEvent> {

    companion object {
        @JvmStatic
        private val logger = LoggerFactory.getLogger(DevFeedFetcherService::class.java)
    }

    @Autowired
    @Qualifier("crawlersExecutorService")
    private lateinit var crawlersExecutorService: ExecutorService

    @Autowired
    @Qualifier("screenshotDownloaderExecutorService")
    private lateinit var screenshotDownloaderExecutorService: ExecutorService

    @Autowired
    @Qualifier("articleExtractorExecutorService")
    private lateinit var articleExtractorExecutorService: ExecutorService

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        logger.info("ApplicationReady => scheduling crawling tasks...")
        CompletableFuture.runAsync {
            triggerRemoteWebsiteCrawling()
            triggerScreenshotUpdater()
        }.exceptionally { t ->
            logger.info(t.message, t)
            null
        }
    }

    @Scheduled(cron = "\${crawlers.task.cron-expression}")
    fun triggerRemoteWebsiteCrawling() {
        try {
            if (!crawlers.isNullOrEmpty()) {
                val futures = crawlers
                        .map { crawler -> CompletableFuture.supplyAsync {
                            logger.debug("Crawling from $crawler...")
                            val articles = crawler.fetchArticles()
                            logger.debug("... Done crawling from $crawler : ${articles.size} articles!")
                            articles
                        } }
                        .flatMap { it.join() }
                        .map { CompletableFuture.supplyAsync(
                                ArticleScreenshotGrabber(dao, it),
                                screenshotDownloaderExecutorService) }
                        .map { it.join() }
                        .map { CompletableFuture.supplyAsync(
                                ArticleExtractor(dao, it),
                                articleExtractorExecutorService
                        ) }
                        .map { it.join() }
                        .map { CompletableFuture.supplyAsync(
                                ArticleUpdater(dao, it), crawlersExecutorService) }
                        .toTypedArray()
                CompletableFuture.allOf(*futures).get() //Wait for all of them to finish
            }
        } catch (e: Exception) {
            logger.warn("Crawling remote websites could not complete successfully - " +
                    "will try again later", e)
        }
    }

    @Scheduled(cron = "\${crawlers.screenshot-updater.task.cron-expression}")
    fun triggerScreenshotUpdater() {
        try {
            val articleIdsWithNoScreenshots = dao.getArticlesWithNoScreenshots()
            logger.info(">>> Inspecting (and trying to update) " +
                    "${articleIdsWithNoScreenshots.size} articles with no screenshots")
            val futures = articleIdsWithNoScreenshots
                    .map {
                        CompletableFuture.supplyAsync(
                                ArticleScreenshotGrabber(dao, it, true),
                                screenshotDownloaderExecutorService)
                    }
                    .map { it.join() }
                    .filter { it.screenshot?.data != null }
                    .map {
                        CompletableFuture.supplyAsync(
                                ArticleUpdater(dao, it, true),
                                crawlersExecutorService)
                    }.toTypedArray()
            CompletableFuture.allOf(*futures).get() //Wait for all of them to finish
            logger.info("<<< Done inspecting and updating ${articleIdsWithNoScreenshots.size} " +
                    "articles with no screenshots. Now, there remains " +
                    "${dao.getArticlesWithNoScreenshots().size} articles with no screenshots " +
                    "=> will check again in a near future.")
        } catch (e: ExecutionException) {
            logger.warn("Updating missing screenshots could not complete successfully - " +
                    "will try again later", e)
        }
    }
}