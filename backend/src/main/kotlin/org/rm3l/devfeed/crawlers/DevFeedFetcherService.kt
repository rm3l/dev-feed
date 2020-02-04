package org.rm3l.devfeed.crawlers

import org.rm3l.devfeed.dal.DevFeedDao
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.PostConstruct

@Service
class DevFeedFetcherService(private val dao: DevFeedDao,
                            private val crawlers: Collection<DevFeedCrawler>? = null): HealthIndicator {

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

    private val initDone = AtomicBoolean(false)

    @PostConstruct
    fun init() {
        logger.info("ApplicationReady => scheduling crawling tasks...")
        try {
            CompletableFuture.runAsync {
                triggerRemoteWebsiteCrawling()
                triggerScreenshotUpdater()
                initDone.set(true)
            }.exceptionally { t ->
                logger.info(t.message, t)
                null
            }
        } catch (e: Exception) {
            logger.warn("init() could not complete successfully - " +
                    "will try again later", e)
        }
    }

    @Scheduled(cron = "\${crawlers.task.cron-expression}")
    @Synchronized fun triggerRemoteWebsiteCrawling() {
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
                        .map {
                            if (!dao.existArticlesByTitleAndUrl(it.title, it.url)) {
                                dao.insertArticle(it)
                            }
                            it
                        }
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
                initDone.set(true)
            }
        } catch (e: Exception) {
            logger.warn("Crawling remote websites could not complete successfully - " +
                    "will try again later", e)
        }
    }

    @Scheduled(cron = "\${crawlers.screenshot-updater.task.cron-expression}")
    @Synchronized fun triggerScreenshotUpdater() {
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
                                ArticleUpdater(dao, it),
                                crawlersExecutorService)
                    }.toTypedArray()
            CompletableFuture.allOf(*futures).get() //Wait for all of them to finish
            initDone.set(true)
            logger.info("<<< Done inspecting and updating ${articleIdsWithNoScreenshots.size} " +
                    "articles with no screenshots. Now, there remains " +
                    "${dao.getArticlesWithNoScreenshots().size} articles with no screenshots " +
                    "=> will check again in a near future.")
        } catch (e: ExecutionException) {
            logger.warn("Updating missing screenshots could not complete successfully - " +
                    "will try again later", e)
        }
    }

    override fun health(): Health =
            if (initDone.get()) {
                Health.up().build()
            } else {
                Health.outOfService().build()
            }
}
