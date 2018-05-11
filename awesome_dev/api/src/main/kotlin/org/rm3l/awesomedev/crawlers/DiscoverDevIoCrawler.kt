package org.rm3l.awesomedev.crawlers

import khttp.get
import org.jsoup.Jsoup
import org.rm3l.awesomedev.dal.AwesomeDevDao
import org.rm3l.awesomedev.utils.asSupportedTimestamp
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.net.URL
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Supplier
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

const val BACKEND_BASE_URL = "http://www.discoverdev.io"
const val BACKEND_ARCHIVE_URL = "$BACKEND_BASE_URL/archive"
const val USER_AGENT = "org.rm3l.discoverdev_io"

data class Article(val id: Long?= null,
                   val timestamp: Long = System.currentTimeMillis(),
                   val title: String,
                   val description: String? = null,
                   val url: String,
                   val domain: String = URL(url).host,
                   var tags: Collection<String>? = setOf(),
                   var screenshot: Screenshot? = null,
                   var parsed: ArticleParsed? = null)

data class Screenshot(val data: String?= null,
                      val height: Int?= null,
                      val width: Int?= null,
                      val mimeType: String?= null)

data class ArticleParsed(val url: String,
                         val title: String? = null,
                         val author: String? = null,
                         val published: String? = null, //TODO Use DateTime
                         val image: String? = null,
                         val videos: Collection<String>? = null,
                         val keywords: Collection<String>? = null,
                         val description: String? = null,
                         val body: String)

@Component
class DiscoverDevIoCrawler(val dao: AwesomeDevDao) {

    companion object {
        @JvmStatic
        private val logger = LoggerFactory.getLogger(DiscoverDevIoCrawler::class.java)
    }

    @Value("\${crawlers.task.thread-pool-size}")
    private lateinit var threadPoolSize: String

    private lateinit var executorService: ExecutorService

    @Value("\${crawlers.screenshot-grabber.task.thread-pool-size}")
    private lateinit var screenshotUpdaterThreadPoolSize: String

    private lateinit var screenshotUpdaterExecutorService: ExecutorService

    private lateinit var articleExtractorExecutorService: ExecutorService

    @PostConstruct
    fun init() {
        this.screenshotUpdaterExecutorService = Executors.newFixedThreadPool(screenshotUpdaterThreadPoolSize.toInt())
        this.executorService = Executors.newFixedThreadPool(threadPoolSize.toInt())
        this.articleExtractorExecutorService = Executors.newFixedThreadPool(screenshotUpdaterThreadPoolSize.toInt())
        CompletableFuture.runAsync {
            triggerRemoteWebsiteCrawling()
            triggerScreenshotUpdater()
        }.exceptionally { t ->
            logger.info(t.message, t)
            null
        }
    }

    @Scheduled(cron = "\${crawlers.screenshot-updater.task.cron-expression}")
    fun triggerScreenshotUpdater() {
        try {
            val articleIdsWithNoScreenshots = dao.getArticlesWithNoScreenshots()
            logger.info(">>> Inspecting (and trying to update) ${articleIdsWithNoScreenshots.size} articles with no screenshots")
            val futures = articleIdsWithNoScreenshots
                    .map {
                        CompletableFuture.supplyAsync(
                                DiscoverDevIoArticleScreenshotGrabber(dao, it, true),
                                screenshotUpdaterExecutorService)
                    }
                    .map { it.join() }
                    .filter { it.screenshot?.data != null }
                    .map {
                        CompletableFuture.supplyAsync(
                                DiscoverDevIoArchiveCrawler(dao, it, true),
                                executorService)
                    }.toTypedArray()
            CompletableFuture.allOf(*futures).get() //Wait for all of them to finish
            logger.info("<<< Done inspecting and updating ${articleIdsWithNoScreenshots.size} articles with no screenshots. " +
                    "Now, there remains ${dao.getArticlesWithNoScreenshots().size} articles with no screenshots " +
                    "=> will check again in a near future.")
        } catch (e: ExecutionException) {
            if (logger.isDebugEnabled) {
                logger.debug("Updating missing screenshots could not complete successfully - will try again later", e)
            }
        }
    }

    @Scheduled(cron = "\${crawlers.task.cron-expression}")
    fun triggerRemoteWebsiteCrawling() {
        try {
            logger.info(">>> Crawling website: $BACKEND_ARCHIVE_URL")
            val futures = Jsoup.connect(BACKEND_ARCHIVE_URL)
                    .userAgent(USER_AGENT)
                    .get()
                    .run {
                        select("main.archive-page ul.archive-list li a")
                                .map { it.attr("href")}
                                .map { it.replaceFirst("/archive/", "", ignoreCase = true) }
                                .map {
                                    CompletableFuture.supplyAsync(
                                            DiscoverDevIoCrawlerArchiveFetcherFutureSupplier(it), executorService) }
                                .flatMap { it.join() }
                                .map { CompletableFuture.supplyAsync(
                                        DiscoverDevIoArticleScreenshotGrabber(dao, it),
                                        screenshotUpdaterExecutorService) }
                                .map { it.join() }
                                .map { CompletableFuture.supplyAsync(
                                        ArticleExtractor(dao, it),
                                        articleExtractorExecutorService
                                ) }
                                .map { it.join() }
                                .map { CompletableFuture.supplyAsync(
                                        DiscoverDevIoArchiveCrawler(dao, it), executorService) }
                                .toTypedArray()
                    }
            CompletableFuture.allOf(*futures).get() //Wait for all of them to finish
            logger.info("<<< Done crawling website: $BACKEND_ARCHIVE_URL")
        } catch (e: ExecutionException) {
            if (logger.isDebugEnabled) {
                logger.debug("Crawling execution could not complete successfully - will try again later", e)
            }
        }
    }

    @PreDestroy
    private fun destroy() {
        executorService.shutdownNow()
    }
}

private class DiscoverDevIoCrawlerArchiveFetcherFutureSupplier(private val date: String):
        Supplier<List<Article>> {

    companion object {
        @JvmStatic
        private val logger = LoggerFactory.getLogger(DiscoverDevIoCrawlerArchiveFetcherFutureSupplier::class.java)
    }

    override fun get(): List<Article> =
            Jsoup.connect("$BACKEND_ARCHIVE_URL/$date")
                    .userAgent(USER_AGENT)
                    .get()
                    .run {
                        val articlesList = select("main.archive-page ul.archive-list li.post-item")
                                .map { element ->
                                    val titleAndLink = element.select("h1.title a")
                                    Article(
                                            timestamp = date.asSupportedTimestamp()!!,
                                            title = titleAndLink.text(),
                                            url = titleAndLink.attr("href"),
                                            description = element.select("p.description").text(),
                                            tags = element.select("p.tags a.tlink")
                                                    .map { tagElement -> tagElement.text() }
                                                    .toSet())
                                }.toList()
                        if (logger.isDebugEnabled) {
                            logger.debug("Fetched ${articlesList.size} articles for $date")
                        }
                        articlesList
                    }
}

/**
 * Remote Website screenshot grabber. Based upon this article: https://shkspr.mobi/blog/2015/11/google-secret-screenshot-api/
 */
private class DiscoverDevIoArticleScreenshotGrabber(private val dao: AwesomeDevDao,
                                                    private val article: Article,
                                                    private val updater: Boolean = false):
        Supplier<Article> {

    companion object {

        private const val GOOGLE_PAGESPEED_URL_FORMAT =
                "https://www.googleapis.com/pagespeedonline/v1/runPagespeed?url=%s&screenshot=true"

        @JvmStatic
        private val logger = LoggerFactory.getLogger(DiscoverDevIoArticleScreenshotGrabber::class.java)
    }

    override fun get(): Article {
        val url = GOOGLE_PAGESPEED_URL_FORMAT.format(article.url)
        try {
            //Check if (title, url) pair already exist in the DB
            if (updater || !dao.existArticlesByTitleAndUrl(article.title, article.url)) {
                val screenshotJsonObject =
                        get(url).jsonObject.optJSONObject("screenshot")
                //Weird, but for reasons best known to Google, / is replaced with _, and + is replaced with -
                val base64ImageData = screenshotJsonObject.optString("data")
                        .replace("_", "/")
                        .replace("-", "+")
                val mimeType = screenshotJsonObject.optString("mime_type")
                val height = screenshotJsonObject.optInt("height")
                val width = screenshotJsonObject.optInt("width")
                if (!base64ImageData.isBlank()) {
                    article.screenshot = Screenshot(data = base64ImageData,
                            mimeType = mimeType,
                            width = width,
                            height = height)
                }
            }
        } catch (e: Exception) {
            if (logger.isDebugEnabled) {
                logger.debug("Could not fetch screenshot data for ${article.url}: $url", e)
            }
        }
        return article
    }

}

private class ArticleExtractor(private val dao: AwesomeDevDao, private val article: Article): Supplier<Article> {

    companion object {

        private const val ARTICLE_EXTRACTION_API_URL_FORMAT = "https://document-parser-api.lateral.io/?url=%s"
        private const val ARTICLE_EXTRACTION_API_SUBSCRIPTION_KEY = "e6c16b3ec541b7ed385921023a9704e1"

        @JvmStatic
        private val logger = LoggerFactory.getLogger(ArticleExtractor::class.java)

        private val debugDone = AtomicBoolean(false)
    }

    override fun get(): Article {
        val url = ARTICLE_EXTRACTION_API_URL_FORMAT.format(article.url)
        try {
            //FIXME Remove in DEBUG mode
//            if (debugDone.getAndSet(true)) {
//                return article
//            }
            if (!dao.existArticleParsed(article.url)) {
                if (logger.isDebugEnabled) {
                    logger.debug("Getting article extraction data from url: $url")
                }
                val articleExtractionJsonObject =
                        get(url,
                                headers = mapOf(
                                        "Content-Type" to "application/json",
                                        "subscription-key" to ARTICLE_EXTRACTION_API_SUBSCRIPTION_KEY
                                )).jsonObject

                val parsedImage = articleExtractionJsonObject.optString("image")

                article.parsed = ArticleParsed(
                        url = article.url,
                        title = articleExtractionJsonObject.optString("title"),
                        published = articleExtractionJsonObject.optString("published"),
                        author = articleExtractionJsonObject.optString("author"),
                        image = if (parsedImage.isNullOrBlank()) null else parsedImage,
                        description = articleExtractionJsonObject.optString("description"),
                        body = articleExtractionJsonObject.optString("body"),
                        videos = articleExtractionJsonObject.optJSONArray("videos")?.map { it.toString() }?.toSet(),
                        keywords = articleExtractionJsonObject.optJSONArray("keywords")?.map { it.toString() }?.toSet()
                )
            }
        } catch (e: Exception) {
            if (logger.isDebugEnabled) {
                logger.debug("Could not fetch article extraction data for ${article.url}: $url", e)
            }
        }
        return article
    }

}

private class DiscoverDevIoArchiveCrawler(private val dao: AwesomeDevDao,
                                          private val article: Article,
                                          private val updater: Boolean = false):
        Supplier<Unit> {

    companion object {
        @JvmStatic
        private val logger = LoggerFactory.getLogger(DiscoverDevIoArchiveCrawler::class.java)
    }

    override fun get() {
        if (logger.isDebugEnabled) {
            logger.debug(">>> Handling article crawled: $article")
        }

        if (updater) {
            dao.updateArticleScreenshotData(article)
        } else {
            //Check if (title, url) pair already exist in the DB
            val existArticlesByTitleAndUrl = dao.existArticlesByTitleAndUrl(article.title, article.url)
            if (logger.isDebugEnabled) {
                logger.debug("$existArticlesByTitleAndUrl = existArticlesByTitleAndUrl(${article.title}, ${article.url})")
            }
            if (!existArticlesByTitleAndUrl) {
                dao.insertArticle(article)
            }
        }
    }
}