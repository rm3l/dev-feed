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

package org.rm3l.devfeed.crawlers

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import org.apache.commons.lang3.concurrent.BasicThreadFactory
import org.rm3l.devfeed.common.articleparser.ArticleExtractor
import org.rm3l.devfeed.common.contract.Article
import org.rm3l.devfeed.common.screenshot.ArticleScreenshotExtractor
import org.rm3l.devfeed.crawlers.common.DevFeedCrawler
import org.rm3l.devfeed.persistence.ArticleUpdater
import org.rm3l.devfeed.persistence.DevFeedDao
import org.rm3l.devfeed.persistence.utils.handleAndPersistIfNeeded
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class DevFeedFetcherService(
    private val dao: DevFeedDao,
    private val crawlers: Collection<DevFeedCrawler>? = null,
    private val articleExtractor: ArticleExtractor? = null,
    private val articleScreenshotExtractor: ArticleScreenshotExtractor? = null
) {

  companion object {
    @JvmStatic private val logger = LoggerFactory.getLogger(DevFeedFetcherService::class.java)
  }

  @Autowired
  @Qualifier("devFeedExecutorService")
  private lateinit var devFeedExecutorService: ExecutorService

  @Value("\${crawlers.task.fetch-articles.max-age-days}")
  private lateinit var articlesMaxAgeDays: String

  private var crawlersExecutor: ExecutorService? = null

  private val remoteWebsiteCrawlingSucceeded = AtomicBoolean(false)
  private val remoteWebsiteCrawlingErrored = AtomicBoolean(false)
  private val screenshotUpdatesSucceeded = AtomicBoolean(false)
  private val screenshotUpdatesErrored = AtomicBoolean(false)

  @PostConstruct
  fun init() {
    logger.info("ApplicationReady => scheduling crawling tasks...")

    val threadFactory = BasicThreadFactory.Builder().namingPattern("crawlers-%d").build()
    this.crawlersExecutor =
        if (crawlers.isNullOrEmpty()) {
          Executors.newSingleThreadExecutor(threadFactory)
        } else {
          Executors.newFixedThreadPool(crawlers.size + 1, threadFactory)
        }

    // Fire and forget to not block the application startup
    CompletableFuture.runAsync(
        { triggerRemoteWebsiteCrawlingAndScreenshotUpdater() }, this.crawlersExecutor)

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
        crawlers
            .map { crawler ->
              CompletableFuture.runAsync(
                  {
                    logger.info("Crawling from $crawler...")
                    val articles = crawler.call()
                    articles.handleAndPersistIfNeeded(
                        dao,
                        devFeedExecutorService,
                        articleScreenshotExtractor,
                        articleExtractor,
                        maxAgeDays =
                            if (articlesMaxAgeDays.isBlank()) null else articlesMaxAgeDays.toLong())
                    logger.info("... Done crawling from $crawler : ${articles.size} articles!")
                  },
                  crawlersExecutor!!)
            }
            .forEach { it.join() }
        logger.warn("Done crawling remote websites successfully")
        remoteWebsiteCrawlingSucceeded.set(true)
        remoteWebsiteCrawlingErrored.set(false)
      }
    } catch (e: Exception) {
      logger.warn(
          "Crawling remote websites could not complete successfully - " + "will try again later", e)
      remoteWebsiteCrawlingErrored.set(true)
      remoteWebsiteCrawlingSucceeded.set(false)
    } finally {
      triggerScreenshotUpdater()
    }
  }

  private fun triggerScreenshotUpdater() {
    if (articleScreenshotExtractor == null) {
      logger.info("No Article Screenshot provider found => skipping screenshot extraction task")
      return
    }
    try {
      val articleIdsWithNoScreenshots = dao.getArticlesWithNoScreenshots()
      logger.info(
          ">>> Inspecting (and trying to update) " +
              "${articleIdsWithNoScreenshots.size} articles with no screenshots")
      val futures =
          articleIdsWithNoScreenshots
              .map { article ->
                CompletableFuture.supplyAsync(
                    {
                      articleScreenshotExtractor.extractScreenshot(article)
                      article
                    },
                    devFeedExecutorService)
              }
              .map { it.join() }
              .filter { it.screenshot?.data != null }
              .map {
                CompletableFuture.supplyAsync(ArticleUpdater(dao, it), devFeedExecutorService)
              }
              .toTypedArray()
      CompletableFuture.allOf(*futures).get() // Wait for all of them to finish
      logger.info(
          "<<< Done inspecting and updating ${articleIdsWithNoScreenshots.size} " +
              "articles with no screenshots. Now, there remains " +
              "${dao.getArticlesWithNoScreenshots().size} articles with no screenshots " +
              "=> will check again in a near future.")
      screenshotUpdatesSucceeded.set(true)
      screenshotUpdatesErrored.set(false)
    } catch (e: ExecutionException) {
      logger.warn(
          "Updating missing screenshots could not complete successfully - " +
              "will try again later",
          e)
      screenshotUpdatesErrored.set(true)
      screenshotUpdatesSucceeded.set(false)
    }
  }
}

class DummyCrawler : DevFeedCrawler() {

  private companion object {
    private const val SOURCE = "https://dev-feed.example.com"
  }

  override fun call(): Collection<Article> {
    val now = System.currentTimeMillis()
    return listOf(
        Article(
            timestamp = now,
            title = "Article Title 1",
            description = "Lorem Ipsum Dolor Sit Amet",
            source = SOURCE,
            url = "$SOURCE/article1",
            tags = listOf("tag1", "tag2"),
        ),
        Article(
            timestamp = now,
            title = "Article Title 2",
            description = "consectetur adipiscing elit",
            source = SOURCE,
            url = "$SOURCE/article2",
            tags = listOf("tag1", "tag3", "tag4"),
        ),
        Article(
            timestamp = now,
            title = "Article Title 3",
            description = "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua",
            source = SOURCE,
            url = "$SOURCE/article3",
            tags = listOf("tag10", "tag3", "tag1"),
        ))
  }
}
