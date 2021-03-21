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

package org.rm3l.devfeed.crawlers.cli.rdbms

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.rm3l.devfeed.crawlers.common.DEFAULT_THREAD_POOL_SIZE
import org.rm3l.devfeed.crawlers.common.DevFeedCrawler
import org.rm3l.devfeed.persistence.impl.rdbms.DevFeedRdbmsDao
import org.rm3l.devfeed.persistence.utils.handleAndPersistIfNeeded
import org.rm3l.devfeed.screenshot.impl.GooglePageSpeedOnlineScreenshotExtractor
import picocli.CommandLine

@CommandLine.Command(
    name = "rdbms", description = ["Fetch and persist articles in a relational data store"])
class DevFeedCrawlerCliRdbms : Runnable {

  @CommandLine.Option(
      names = ["-c", "--crawler"], description = ["the crawler canonical class name"])
  private lateinit var crawlerType: Class<DevFeedCrawler>

  @CommandLine.Option(
      names = ["-t", "--thread-pool-size"],
      description = ["the thread pool size for fetching articles (default: \${DEFAULT-VALUE})"])
  private var threadPoolSize: String = "$DEFAULT_THREAD_POOL_SIZE"

  @CommandLine.Option(names = ["-j", "--datasource-jdbc-url"], description = ["the JDBC URL"])
  private lateinit var datasourceUrl: String

  @CommandLine.Option(names = ["-d", "--datasource-jdbc-driver"], description = ["the JDBC URL"])
  private lateinit var datasourceDriver: String

  @CommandLine.Option(
      names = ["-u", "--datasource-user"], description = ["the datasource username"])
  private lateinit var datasourceUser: String

  @CommandLine.Option(
      names = ["-p", "--datasource-password"], description = ["the datasource password"])
  private lateinit var datasourcePassword: String

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

    DevFeedRdbmsDao(
            datasourceUrl = datasourceUrl,
            datasourceDriver = datasourceDriver,
            datasourceUser = datasourceUser,
            datasourcePassword = datasourcePassword)
        .use { dao ->
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
