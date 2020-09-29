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
package org.rm3l.devfeed.crawlers.engineeringblogs_xyz

import com.rometools.opml.feed.opml.Opml
import com.rometools.opml.feed.opml.Outline
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.WireFeedInput
import com.rometools.rome.io.XmlReader
import org.rm3l.devfeed.common.contract.Article
import org.rm3l.devfeed.common.utils.asSupportedTimestamp
import org.rm3l.devfeed.crawlers.common.DEFAULT_THREAD_POOL_SIZE
import org.rm3l.devfeed.crawlers.common.DevFeedCrawler
import org.slf4j.LoggerFactory
import java.net.URL
import java.io.ByteArrayInputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

class EngineeringBlogsCrawler(
  private val executorService: ExecutorService = Executors.newFixedThreadPool(DEFAULT_THREAD_POOL_SIZE)
) : DevFeedCrawler {

  companion object {
    @JvmStatic
    private val logger = LoggerFactory.getLogger(EngineeringBlogsCrawler::class.java)

    private const val OPML_URL = "https://engblogs.s3.amazonaws.com/engblogs.opml"
  }

  override fun getCrawlerSource() = "http://engineeringblogs.xyz"

  @Throws(Exception::class)
  override fun fetchArticles(): Collection<Article> {
    try {
      logger.info(">>> Getting Feed Outlines from : $OPML_URL")

      val start = System.nanoTime()

      val opmlFileContent = URL(OPML_URL).readText()

      val opml = WireFeedInput().build(XmlReader(ByteArrayInputStream(opmlFileContent.toByteArray()))) as Opml

      val articles = opml
        .outlines
        .filterNot { it.isComment }
        .firstOrNull()
        ?.children
        ?.filterNotNull()
        ?.filterNot { it.isComment }
        ?.filter { "rss".equals(it.type, ignoreCase = true) }
        ?.map {
          logger.trace("Handling RSS feed for '${it.title}': ${it.xmlUrl}")
          CompletableFuture.supplyAsync(
            EngineeringBlogsCrawlerArchiveFetcherFutureSupplier(it),
            executorService)
        }
        ?.flatMap { it.join() }
        ?.toList()
      logger.info("<<< Done handling Feed Outlines from: $OPML_URL" +
        " in ${TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)}ms" +
        " => ${articles?.size} articles")
      return articles ?: emptyList()
    } catch (e: ExecutionException) {
      logger.warn("Crawling execution could not complete successfully - " +
        "will try again later", e)
      throw e
    }
  }
}

private class EngineeringBlogsCrawlerArchiveFetcherFutureSupplier(private val outline: Outline) :
  Supplier<Collection<Article>> {

  companion object {
    @JvmStatic
    private val logger = LoggerFactory.getLogger(EngineeringBlogsCrawlerArchiveFetcherFutureSupplier::class.java)
  }

  override fun get(): Collection<Article> {
    try {
      val articles = SyndFeedInput().build(XmlReader(ByteArrayInputStream(URL(outline.xmlUrl).readText().toByteArray())))
        .entries
        .filterNot { it.publishedDate == null && it.updatedDate == null }
        .map { feedEntry ->
          Article(
            source = "https://engineeringblogs.xyz/",
            timestamp = (feedEntry.publishedDate ?: feedEntry.updatedDate)
              .asSupportedTimestamp()!!,
            title = feedEntry.title,
            url = feedEntry.link ?: outline.htmlUrl,
            description = feedEntry.description?.value,
            tags = feedEntry.categories?.map { it -> it.name } ?: emptyList()

          )
        }.toList()

      if (logger.isDebugEnabled) {
        logger.trace("Fetched ${articles.size} articles for ${outline.title} / ${outline.xmlUrl}")
      }

      return articles
    } catch (e: Exception) {
      logger.warn("Error while fetching articles for ${outline.title} / ${outline.xmlUrl}: ${e.message}")
      if (logger.isDebugEnabled) {
        logger.debug(e.message, e)
      }
      return emptyList()
    }
  }
}
