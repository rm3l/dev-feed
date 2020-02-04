//The MIT License (MIT)
//
//Copyright (c) 2019 Armel Soro
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to deal
//in the Software without restriction, including without limitation the rights
//to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in all
//copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//SOFTWARE.
package org.rm3l.devfeed.crawlers.impl.discoverdev_io

import org.jsoup.Jsoup
import org.rm3l.devfeed.contract.Article
import org.rm3l.devfeed.crawlers.DevFeedCrawler
import org.rm3l.devfeed.utils.asSupportedTimestamp
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

const val BACKEND_BASE_URL = "https://www.discoverdev.io"
const val BACKEND_ARCHIVE_URL = "$BACKEND_BASE_URL/archive"
const val USER_AGENT = "org.rm3l.devfeed"

@Service
class DiscoverDevIoCrawler: DevFeedCrawler {

    companion object {
        @JvmStatic
        private val logger = LoggerFactory.getLogger(DiscoverDevIoCrawler::class.java)
    }
    
    @Autowired
    @Qualifier("crawlersExecutorService")
    private lateinit var crawlersExecutorService: ExecutorService

    @Throws(Exception::class)
    override fun fetchArticles(): Collection<Article> {
        try {
            logger.info(">>> Crawling website: $BACKEND_ARCHIVE_URL")

            val start = System.nanoTime()
            val articles = Jsoup.connect(BACKEND_ARCHIVE_URL)
                    .userAgent(USER_AGENT)
                    .get()
                    .run {
                        select("main.archive-page ul.archive-list li a")
                                .map { it.attr("href")}
                                .map { it.replaceFirst("/archive/", "", ignoreCase = true) }
                                .map {
                                    logger.trace("Crawling page: $it ...")
                                    CompletableFuture.supplyAsync(
                                            DiscoverDevIoCrawlerArchiveFetcherFutureSupplier(it),
                                            crawlersExecutorService) }
                                .flatMap { it.join() }
                                .toList()
                    }
            logger.info("<<< Done crawling website: $BACKEND_ARCHIVE_URL" +
                    " in ${TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)}ms" +
                    " => ${articles.size} articles")
            return articles
        } catch (e: ExecutionException) {
            logger.warn("Crawling execution could not complete successfully - " +
                    "will try again later", e)
            throw e
        }
    }
}

private class DiscoverDevIoCrawlerArchiveFetcherFutureSupplier(private val date: String):
        Supplier<Collection<Article>> {

    companion object {
        @JvmStatic
        private val logger = LoggerFactory.getLogger(DiscoverDevIoCrawlerArchiveFetcherFutureSupplier::class.java)
    }

    override fun get(): Collection<Article> {
	    try {
            return Jsoup.connect("$BACKEND_ARCHIVE_URL/$date")
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
                            logger.trace("Fetched ${articlesList.size} articles for $date")
                        }
                        articlesList
                    }
        } catch (e: Exception) {
            logger.warn("Error while fetching articles for $date: ${e.message}")
            if (logger.isDebugEnabled) {
                logger.debug(e.message, e)
            }
	        return emptyList()
        }
    }
}
