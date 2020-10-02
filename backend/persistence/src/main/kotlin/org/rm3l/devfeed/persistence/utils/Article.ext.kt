package org.rm3l.devfeed.persistence.utils

import org.rm3l.devfeed.common.articleparser.ArticleExtractor
import org.rm3l.devfeed.common.contract.Article
import org.rm3l.devfeed.common.screenshot.ArticleScreenshotExtractor
import org.rm3l.devfeed.persistence.ArticleUpdater
import org.rm3l.devfeed.persistence.DevFeedDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService

val logger: Logger = LoggerFactory.getLogger("handleArticles")

fun Collection<Article>?.handleAndPersistIfNeeded(dao: DevFeedDao,
                                                  executorService: ExecutorService,
                                                  articleScreenshotExtractor: ArticleScreenshotExtractor? = null,
                                                  articleParser: ArticleExtractor? = null,
                                                  synchronous: Boolean = true):
  Collection<CompletableFuture<Unit>> {
  val futures = this
    ?.map { article ->
      CompletableFuture.supplyAsync({
        article.tags = article.tags?.filterNotNull() ?: emptyList()
        if (!dao.existArticlesByUrl(article.url)) {
          val identifier = dao.insertArticle(article)
          dao.findArticleById(identifier)
        } else {
          dao.findArticleByUrl(article.url)
        }
      },
        executorService)
        .exceptionally {
          logger.warn("Could not insert article for $article", it)
          null
        }
    }?.mapNotNull { it.join() }
    ?.map { article ->
      if (articleScreenshotExtractor != null && article.screenshot == null) {
        CompletableFuture.supplyAsync({
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
        CompletableFuture.supplyAsync({
          articleParser.extractArticleData(article)
          article
        }, executorService
        )
      } else {
        CompletableFuture.completedFuture(article)
      }
    }
    ?.map { it.join() }
    ?.map {
      CompletableFuture.supplyAsync(
        ArticleUpdater(dao, it), executorService)
        .exceptionally { exception ->
          logger.warn("Could not insert article for $it", exception)
        }
    }
    ?.toList() ?: listOf()

  if (synchronous) {
    CompletableFuture.allOf(*futures.toTypedArray()).get() //Wait for all of them to finish
  }

  return futures
}
