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

package org.rm3l.devfeed.persistence

import java.io.Closeable
import org.rm3l.devfeed.common.contract.Article
import org.rm3l.devfeed.common.contract.ArticleFilter

interface DevFeedDao : Closeable {
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
  fun allButRecentArticles(
      limit: Int? = null,
      offset: Long? = null,
      filter: ArticleFilter? = null
  ): Collection<Article>
  fun getArticles(
      limit: Int? = null,
      offset: Long? = null,
      filter: ArticleFilter? = null
  ): Collection<Article>
  fun getArticlesDates(limit: Int? = null, offset: Long? = null): Set<Long>
  fun getRecentArticles(limit: Int? = null, offset: Long? = null): Collection<Article>
  fun getTags(
      limit: Int? = null,
      offset: Long? = null,
      search: Collection<String>?
  ): Collection<String>

  @Throws(Exception::class) fun checkHealth()
}
