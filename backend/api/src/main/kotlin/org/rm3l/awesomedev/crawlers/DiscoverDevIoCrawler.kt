package org.rm3l.awesomedev.crawlers

import org.jsoup.Jsoup
import org.rm3l.awesomedev.dal.AwesomeDevDao
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.function.Supplier
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

const val BACKEND_BASE_URL = "http://www.discoverdev.io"
const val BACKEND_ARCHIVE_URL = "$BACKEND_BASE_URL/archive"
const val USER_AGENT = "org.rm3l.discoverdev_io"

data class Article(val date: String,
                   val title: String,
                   var description: String? = null,
                   var url: String,
                   val tags: Collection<String>? = setOf())

@Component
class DiscoverDevIoCrawler(val dao: AwesomeDevDao) {

    private val executorService = Executors.newFixedThreadPool(50)

    companion object {
        @JvmStatic
        private val logger = LoggerFactory.getLogger(DiscoverDevIoCrawler::class.java)
    }

    @PostConstruct
    fun init() {
        this.triggerRemoteWebsiteCrawling()
    }

    @Scheduled(cron = "\${crawling.refresh.cron.expression}")
    fun triggerRemoteWebsiteCrawling() {
        logger.info(">>> Crawling website: $BACKEND_ARCHIVE_URL")
        val futures = Jsoup.connect(BACKEND_ARCHIVE_URL)
            .userAgent(USER_AGENT)
            .get()
            .run {
                select("main.archive-page ul.archive-list li a")
                        .map {
                            it.attr("href").replaceFirst("/archive/", "", ignoreCase = true)
                        }.map { CompletableFuture.supplyAsync(
                                    DiscoverDevIoCrawlerArchiveFetcherFutureSupplier(it),
                                    executorService)
                        }
                        .flatMap { it.join() }
                        .map { CompletableFuture.supplyAsync(DiscoverDevIoArchiveCrawler(dao, it), executorService) }
                        .toTypedArray()
            }
        CompletableFuture.allOf(*futures).get() //Wait for all of them to finish
        logger.info("<<< Done crawling website: $BACKEND_ARCHIVE_URL")
    }

    @PreDestroy
    private fun destroy() {
        executorService.shutdownNow()
    }
}

internal class DiscoverDevIoCrawlerArchiveFetcherFutureSupplier(private val date: String):
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
                        select("main.archive-page ul.archive-list li.post-item")
                                .map { element ->
                                    val titleAndLink = element.select("h1.title a")
                                    val title = titleAndLink.text()
                                    val url = titleAndLink.attr("href")

                                    val tags = element.select("p.tags a.tlink")
                                            .map { tagElement -> tagElement.text() }
                                            .toSet()
                                    if (logger.isDebugEnabled) {
                                        logger.debug("$date - $title ($url) / $tags")
                                    }
                                    Article(date = date,
                                            title = title,
                                            url = url,
                                            tags = tags)
                                }.toList()
                    }

}

internal class DiscoverDevIoArchiveCrawler(private val dao: AwesomeDevDao, private val article: Article):
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
            dao.insertArticleAndTags(article.date, article.title, article.description, article.url, article.tags)
        }
    }
}