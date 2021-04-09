/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Armel Soro
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.rm3l.devfeed.crawlers.cli.mongodb

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.rm3l.devfeed.crawlers.common.DEFAULT_THREAD_POOL_SIZE
import org.rm3l.devfeed.crawlers.common.DevFeedCrawler
import org.rm3l.devfeed.persistence.impl.mongodb.DevFeedMongoDbDao
import org.rm3l.devfeed.persistence.utils.handleAndPersistIfNeeded
import org.rm3l.devfeed.screenshot.impl.GooglePageSpeedOnlineScreenshotExtractor
import picocli.CommandLine

@CommandLine.Command(
    name = "mongo", description = ["Fetch and persist articles in a MongoDB document store"])
class DevFeedCrawlerCliMongo : Runnable {

  @CommandLine.Option(
      names = ["-c", "--crawler"], description = ["the crawler canonical class name"])
  private lateinit var crawlerType: Class<DevFeedCrawler>

  @CommandLine.Option(
      names = ["-t", "--thread-pool-size"],
      description = ["the thread pool size for fetching articles (default: \${DEFAULT-VALUE})"])
  private var threadPoolSize: String = "$DEFAULT_THREAD_POOL_SIZE"

  @CommandLine.Option(
      names = ["-M", "--mongo-connection-string"], description = ["the MongoDB Connection String"])
  private lateinit var connectionString: String

  @CommandLine.Option(
      names = ["-s", "--screenshot-parser-pagespeed-online-key"],
      description = ["the PageSpeed Online API Key for screenshot extraction"])
  private var screenshotPageSpeedOnlineKey: String? = null

  @CommandLine.Option(
      names = ["-m", "--article-max-age-days"],
      description = ["articles older than this max age will not be persisted"])
  private var articleMaxAgeDays: Long? = null

  override fun run() {
    val executorService = Executors.newFixedThreadPool(threadPoolSize.toInt())
    val crawler =
        try {
          crawlerType.getConstructor(ExecutorService::class.java).newInstance(executorService)
        } catch (nsme: NoSuchMethodException) {
          // Default to default constructor, if any
          crawlerType.getConstructor().newInstance()
        }

    DevFeedMongoDbDao(connectionString = connectionString).use { dao ->
      val articles = crawler.call()
      println("Fetched ${articles?.size} articles using $crawlerType")

      articles.handleAndPersistIfNeeded(
          dao = dao,
          executorService = executorService,
          maxAgeDays = articleMaxAgeDays,
          articleScreenshotExtractor =
              if (screenshotPageSpeedOnlineKey.isNullOrBlank()) null
              else
                  GooglePageSpeedOnlineScreenshotExtractor(
                      dao = dao,
                      pageSpeedOnlineApiKey = screenshotPageSpeedOnlineKey!!,
                      pageSpeedOnlineTimeoutSeconds = 10),
          synchronous = true)
    }
  }
}
