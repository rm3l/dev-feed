package org.rm3l.awesomedev.crawlers

import khttp.get
import org.jsoup.Jsoup
import org.rm3l.awesomedev.dal.AwesomeDevDao
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.net.URL
import java.util.concurrent.*
import java.util.function.Supplier
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

const val BACKEND_BASE_URL = "http://www.discoverdev.io"
const val BACKEND_ARCHIVE_URL = "$BACKEND_BASE_URL/archive"
const val USER_AGENT = "org.rm3l.discoverdev_io"

data class Article(val id: Long?= null,
                   val date: String,
                   val title: String,
                   val description: String? = null,
                   val url: String,
                   val domain: String = URL(url).host,
                   var tags: Collection<String>? = setOf(),
                   var screenshot: Screenshot? = null)

data class Screenshot(val data: String?= null,
                      val height: Int?= null,
                      val width: Int?= null,
                      val mimeType: String?= null)

@Component
class DiscoverDevIoCrawler(val dao: AwesomeDevDao) {

    companion object {
        @JvmStatic
        private val logger = LoggerFactory.getLogger(DiscoverDevIoCrawler::class.java)
    }

    @Value("\${crawlers.task.thread-pool-size}")
    private lateinit var threadPoolSize: String

    private lateinit var executorService: ExecutorService

    @PostConstruct
    fun init() {
        this.executorService = Executors.newFixedThreadPool(threadPoolSize.toInt())
        this.triggerRemoteWebsiteCrawling()
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
                                        DiscoverDevIoArticleScreenshotGrabber(dao, it), executorService) }
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
                                    Article(date = date,
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

private class DiscoverDevIoArticleScreenshotGrabber(private val dao: AwesomeDevDao, private val article: Article):
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
            val existArticlesByTitleAndUrl = dao.existArticlesByTitleAndUrl(article.title, article.url)
            if (logger.isDebugEnabled) {
                logger.debug("$existArticlesByTitleAndUrl = existArticlesByTitleAndUrl(${article.title}, ${article.url})")
            }
            if (!existArticlesByTitleAndUrl) {
                val screenshotJsonObject = get(url).jsonObject.getJSONObject("screenshot")
                val base64ImageData = screenshotJsonObject.getString("data")
                val mimeType = screenshotJsonObject.getString("mime_type")
                val height = screenshotJsonObject.getInt("height")
                val width = screenshotJsonObject.getInt("width")
                if (!base64ImageData.isNullOrBlank()) {
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

private class DiscoverDevIoArchiveCrawler(private val dao: AwesomeDevDao, private val article: Article):
        Supplier<Unit> {

    companion object {
        @JvmStatic
        private val logger = LoggerFactory.getLogger(DiscoverDevIoArchiveCrawler::class.java)
    }

    override fun get() {
        if (logger.isDebugEnabled) {
            logger.debug(">>> Handling article crawled: $article")
        }

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