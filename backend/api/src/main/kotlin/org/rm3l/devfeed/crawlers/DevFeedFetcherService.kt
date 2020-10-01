package org.rm3l.devfeed.crawlers

import org.apache.commons.lang3.concurrent.BasicThreadFactory
import org.rm3l.devfeed.common.contract.Article
import org.rm3l.devfeed.crawlers.common.DevFeedCrawler
import org.rm3l.devfeed.extractors.article.ArticleExtractor
import org.rm3l.devfeed.extractors.screenshot.ArticleScreenshotExtractor
import org.rm3l.devfeed.persistence.DevFeedDao
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
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Supplier
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

@Service
class DevFeedFetcherService(private val dao: DevFeedDao,
                            private val crawlers: Collection<DevFeedCrawler>? = null,
                            private val articleExtractor: ArticleExtractor? = null,
                            private val articleScreenshotExtractor: ArticleScreenshotExtractor? = null) : HealthIndicator {

    companion object {
        @JvmStatic
        private val logger = LoggerFactory.getLogger(DevFeedFetcherService::class.java)
    }

    @Autowired
    @Qualifier("devFeedExecutorService")
    private lateinit var devFeedExecutorService: ExecutorService

    private var crawlersExecutor: ExecutorService? = null

    private val remoteWebsiteCrawlingSucceeded = AtomicBoolean(false)
    private val remoteWebsiteCrawlingErrored = AtomicBoolean(false)
    private val screenshotUpdatesSucceeded = AtomicBoolean(false)
    private val screenshotUpdatesErrored = AtomicBoolean(false)

    @PostConstruct
    fun init() {
        logger.info("ApplicationReady => scheduling crawling tasks...")

        val threadFactory = BasicThreadFactory.Builder().namingPattern("crawlers-%d").build()
        this.crawlersExecutor = if (crawlers.isNullOrEmpty()) {
            Executors.newSingleThreadExecutor(threadFactory)
        } else {
            Executors.newFixedThreadPool(crawlers.size + 1, threadFactory)
        }

        CompletableFuture.runAsync(
                Runnable {
                    triggerRemoteWebsiteCrawlingAndScreenshotUpdater()
                }, this.crawlersExecutor)
                .exceptionally {
                    logger.info(it.message, it)
                    null
                }

        return
    }

    @PreDestroy
    fun destroy() {
        crawlersExecutor?.shutdownNow()
    }

    @Scheduled(cron = "\${crawlers.task.cron-expression}")
    @Synchronized
    fun triggerRemoteWebsiteCrawlingAndScreenshotUpdater() {
        try {
            if (!crawlers.isNullOrEmpty()) {
                crawlers.map { crawler ->
                    CompletableFuture.runAsync(
                            Runnable {
                                logger.debug("Crawling from $crawler...")
                                val articles = crawler.fetchArticles()
                                handleArticles(articles, synchronous = true)
                                logger.debug("... Done crawling from $crawler : ${articles.size} articles!")
                            }, crawlersExecutor!!)
                }.forEach { it.join() }
                logger.warn("Done crawling remote websites successfully")
                remoteWebsiteCrawlingSucceeded.set(true)
                remoteWebsiteCrawlingErrored.set(false)
            }
        } catch (e: Exception) {
            logger.warn("Crawling remote websites could not complete successfully - " +
                    "will try again later", e)
            remoteWebsiteCrawlingErrored.set(true)
            remoteWebsiteCrawlingSucceeded.set(false)
        } finally {
            triggerScreenshotUpdater()
        }
    }

    fun handleArticles(articles: Collection<Article>, synchronous: Boolean = true):
            Collection<CompletableFuture<Unit>> {
        val futures = articles
                .asSequence()
                .map { article ->
                    CompletableFuture.supplyAsync(
                            Supplier {
                                article.tags = article.tags?.filterNotNull() ?: emptyList()
                                if (!dao.existArticlesByUrl(article.url)) {
                                    val identifier = dao.insertArticle(article)
                                    dao.findArticleById(identifier)
                                } else {
                                    dao.findArticleByUrl(article.url)
                                }
                            },
                            devFeedExecutorService)
                            .exceptionally {
                                logger.warn("Could not insert article for $article", it)
                                null
                            }
                }.mapNotNull { it.join() }
                .map { article ->
                    CompletableFuture.supplyAsync(
                            Supplier {
                                if (articleScreenshotExtractor != null && article.screenshot == null) {
                                    articleScreenshotExtractor.extractScreenshot(article)
                                }
                                article
                            },
                            devFeedExecutorService)
                }
                .map { it.join() }
                .map { article ->
                    CompletableFuture.supplyAsync(
                            Supplier {
                                if (articleExtractor != null && article.parsed == null) {
                                    articleExtractor.extractArticleData(article)
                                }
                                article
                            },
                            devFeedExecutorService
                    )
                }
                .map { it.join() }
                .map {
                    CompletableFuture.supplyAsync(
                            ArticleUpdater(dao, it), devFeedExecutorService)
                            .exceptionally { exception ->
                                logger.warn("Could not insert article for $it", exception)
                            }
                }
                .toList()
        if (synchronous) {
            CompletableFuture.allOf(*futures.toTypedArray()).get() //Wait for all of them to finish
        }
        return futures
    }

    private fun triggerScreenshotUpdater() {
        if (articleScreenshotExtractor == null) {
            logger.info("No Article Screenshot provider found => skipping screenshot extraction task")
            return
        }
        try {
            val articleIdsWithNoScreenshots = dao.getArticlesWithNoScreenshots()
            logger.info(">>> Inspecting (and trying to update) " +
                    "${articleIdsWithNoScreenshots.size} articles with no screenshots")
            val futures = articleIdsWithNoScreenshots
                    .map { article ->
                        CompletableFuture.supplyAsync(
                                Supplier {
                                    articleScreenshotExtractor.extractScreenshot(article)
                                    article
                                },
                                devFeedExecutorService)
                    }
                    .map { it.join() }
                    .filter { it.screenshot?.data != null }
                    .map {
                        CompletableFuture.supplyAsync(
                                ArticleUpdater(dao, it),
                                devFeedExecutorService)
                    }.toTypedArray()
            CompletableFuture.allOf(*futures).get() //Wait for all of them to finish
            logger.info("<<< Done inspecting and updating ${articleIdsWithNoScreenshots.size} " +
                    "articles with no screenshots. Now, there remains " +
                    "${dao.getArticlesWithNoScreenshots().size} articles with no screenshots " +
                    "=> will check again in a near future.")
            screenshotUpdatesSucceeded.set(true)
            screenshotUpdatesErrored.set(false)
        } catch (e: ExecutionException) {
            logger.warn("Updating missing screenshots could not complete successfully - " +
                    "will try again later", e)
            screenshotUpdatesErrored.set(true)
            screenshotUpdatesSucceeded.set(false)
        }
    }

    override fun health(): Health =
            if (dao.getRecentArticles(limit = 1).isNotEmpty()) {
                Health.up().build()
            } else {
                Health.down().build()
            }
}
