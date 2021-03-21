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

package org.rm3l.devfeed.persistence.utils

import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import org.rm3l.devfeed.common.articleparser.ArticleExtractor
import org.rm3l.devfeed.common.contract.Article
import org.rm3l.devfeed.common.screenshot.ArticleScreenshotExtractor
import org.rm3l.devfeed.persistence.ArticleUpdater
import org.rm3l.devfeed.persistence.DevFeedDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val logger: Logger = LoggerFactory.getLogger("handleArticles")

fun Collection<Article>?.handleAndPersistIfNeeded(
    dao: DevFeedDao,
    executorService: ExecutorService,
    articleScreenshotExtractor: ArticleScreenshotExtractor? = null,
    articleParser: ArticleExtractor? = null,
    maxAgeDays: Long? = null,
    synchronous: Boolean = true
): Collection<CompletableFuture<Unit>> {

  val start = System.nanoTime()

  val futures =
      this?.asSequence()
          ?.filter { article ->
            if (maxAgeDays == null || maxAgeDays <= 0) {
              true
            } else {
              if (abs(
                  Duration.between(
                          Instant.ofEpochMilli(System.currentTimeMillis()),
                          Instant.ofEpochMilli(article.timestamp))
                      .toDays()) <= maxAgeDays) {
                true
              } else {
                logger.debug(
                    "Skipped Article ${article.url} because it is older than $maxAgeDays days: {}",
                    article.timestamp)
                false
              }
            }
          }
          ?.map { article ->
            CompletableFuture.supplyAsync(
                    {
                      article.tags = article.tags?.filterNotNull() ?: emptyList()
                      var articleOnFile = dao.findArticleByUrl(article.url)
                      if (articleOnFile == null) {
                        logger.info("Inserting new article: ${article.url}")
                        val identifier = dao.insertArticle(article)
                        articleOnFile = dao.findArticleById(identifier)
                      }
                      articleOnFile
                    },
                    executorService)
                .exceptionally {
                  logger.warn("Could not insert article for $article", it)
                  null
                }
          }
          ?.mapNotNull { it.join() }
          ?.map { article ->
            if (articleScreenshotExtractor != null && article.screenshot == null) {
              CompletableFuture.supplyAsync(
                  {
                    logger.info("Extracting screenshot for article, if any: ${article.url}")
                    articleScreenshotExtractor.extractScreenshot(article)
                    article
                  },
                  executorService)
            } else {
              CompletableFuture.completedFuture(article)
            }
          }
          ?.map { it.join() }
          ?.map { article ->
            if (articleParser != null && article.parsed == null) {
              CompletableFuture.supplyAsync(
                  {
                    logger.info("Extract article data: ${article.url}")
                    articleParser.extractArticleData(article)
                    article
                  },
                  executorService)
            } else {
              CompletableFuture.completedFuture(article)
            }
          }
          ?.map { it.join() }
          ?.map {
            CompletableFuture.supplyAsync(
                    {
                      logger.info("Updating article as needed: ${it.url}")
                      ArticleUpdater(dao, it).get()
                    },
                    executorService)
                .exceptionally { exception ->
                  logger.warn("Could not insert article for $it", exception)
                }
          }
          ?.toList()
          ?: listOf()

  if (synchronous) {
    CompletableFuture.allOf(*futures.toTypedArray()).get() // Wait for all of them to finish
  }

  val duration = System.nanoTime() - start

  logger.info(
      "Done handling ${this?.size ?: 0} article(s) in {} minutes ({} ms)",
      TimeUnit.NANOSECONDS.toMinutes(duration),
      TimeUnit.NANOSECONDS.toMillis(duration))

  return futures
}
