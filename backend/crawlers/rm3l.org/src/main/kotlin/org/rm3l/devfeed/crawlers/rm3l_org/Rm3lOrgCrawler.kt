//The MIT License (MIT)
//
//Copyright (c) 2020 Armel Soro
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
package org.rm3l.devfeed.crawlers.rm3l_org

import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import org.rm3l.devfeed.common.contract.Article
import org.rm3l.devfeed.common.utils.asSupportedTimestamp
import org.rm3l.devfeed.crawlers.cli.DevFeedCrawlerCliRunner
import org.rm3l.devfeed.crawlers.common.DevFeedCrawler
import org.slf4j.LoggerFactory
import java.net.URL
import java.io.ByteArrayInputStream
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class Rm3lOrgCrawler : DevFeedCrawler() {

  companion object {
    @JvmStatic
    private val logger = LoggerFactory.getLogger(Rm3lOrgCrawler::class.java)

    private const val RSS_FEED_URL = "https://rm3l.org/rss/"

    @JvmStatic
    fun main(args: Array<String>) {
      val cliArgs = buildCliArgs(Rm3lOrgCrawler::class, args)
      DevFeedCrawlerCliRunner.main(*cliArgs.toTypedArray())
    }
  }

  @Throws(Exception::class)
  override fun call(): Collection<Article> {
    try {
      logger.info(">>> Getting Articles from : $RSS_FEED_URL")

      val start = System.nanoTime()

      val articles = SyndFeedInput()
        .build(XmlReader(ByteArrayInputStream(URL(RSS_FEED_URL).readText()
          .replace("https://cms.rm3l.org", "https://rm3l.org", ignoreCase = true)
          .toByteArray())))
        .entries
        .filterNot { it.publishedDate == null && it.updatedDate == null }
        .map { feedEntry ->
          Article(
            source = "https://rm3l.org/",
            timestamp = (feedEntry.publishedDate ?: feedEntry.updatedDate)
              .asSupportedTimestamp()!!,
            title = feedEntry.title,
            url = feedEntry.link ?: RSS_FEED_URL,
            description = feedEntry.description?.value,
            tags = feedEntry.categories?.map { it -> it.name } ?: emptyList()

          )
        }.toList()

      if (logger.isDebugEnabled) {
        logger.trace("Fetched ${articles.size} articles for $RSS_FEED_URL")
      }

      logger.info("<<< Done handling Feed articles from: $RSS_FEED_URL" +
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

