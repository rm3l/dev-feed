package org.rm3l.devfeed.persistence

import org.rm3l.devfeed.common.contract.Article
import org.rm3l.devfeed.common.contract.ArticleFilter

interface DevFeedDao {
  fun existArticlesByTitleAndUrl(title: String, url: String): Boolean
  fun existArticlesByUrl(url: String): Boolean
  fun deleteByTitleAndUrl(title: String, url: String): Int
  fun existArticleParsed(url: String): Boolean
  fun existTagByName(name: String): Boolean
  fun findArticleById(articleId: String): Article?
  fun findArticleByUrl(url: String): Article?
  fun insertArticle(article: Article): String
  fun shouldRequestScreenshot(title: String, url: String): Boolean
  fun shouldRequestScreenshot(articleId: String): Boolean
  fun updateArticleScreenshotData(article: Article)
  fun getArticlesWithNoScreenshots(): Collection<Article>
  fun allButRecentArticles(limit: Int? = null, offset: Long? = null, filter: ArticleFilter? = null): Collection<Article>
  fun getArticles(limit: Int? = null, offset: Long? = null, filter: ArticleFilter? = null): Collection<Article>
  fun getArticlesDates(limit: Int? = null, offset: Long? = null): Set<Long>
  fun getRecentArticles(limit: Int? = null, offset: Long? = null): Collection<Article>
  fun getTags(limit: Int? = null, offset: Long? = null, search: Collection<String>?): Collection<String>

  @Throws(Exception::class)
  fun checkHealth()
}
