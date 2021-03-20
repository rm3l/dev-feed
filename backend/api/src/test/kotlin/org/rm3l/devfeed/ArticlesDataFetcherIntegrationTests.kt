/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 Armel Soro
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

package org.rm3l.devfeed

import com.netflix.graphql.dgs.DgsQueryExecutor
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration
import java.util.concurrent.ConcurrentHashMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rm3l.devfeed.common.contract.Article
import org.rm3l.devfeed.common.contract.ArticleFilter
import org.rm3l.devfeed.graphql.fetchers.ArticlesDataFetcher
import org.rm3l.devfeed.graphql.scalars.LongScalarRegistration
import org.rm3l.devfeed.persistence.DevFeedDao
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Repository
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(
    classes =
        [
            ArticlesDataFetcherIntegrationTestsDao::class,
            DgsAutoConfiguration::class,
            LongScalarRegistration::class,
            ArticlesDataFetcher::class])
@ActiveProfiles(value = ["test"])
class ArticlesDataFetcherIntegrationTests {

  @Autowired private lateinit var dgsQueryExecutor: DgsQueryExecutor

  @Test
  fun `test articleDates`() {
    val data: List<Int> =
        dgsQueryExecutor.executeAndExtractJsonPath(
            """
        {
          articleDates
        }
      """, "data.articleDates[*]")
    assertEquals(3, data.size)
    assertTrue(data.contains(123))
    assertTrue(data.contains(456))
    assertTrue(data.contains(789))
  }

  @Test
  fun `test articles`() {
    val data: List<Article> =
        dgsQueryExecutor.executeAndExtractJsonPath(
            """
        {
          articles {
            id
            title
            url
          }
        }
      """,
            "data.articles[*]")
    assertEquals(3, data.size)
  }

  @Test
  fun `test articles with limit`() {
    val data: List<Article> =
        dgsQueryExecutor.executeAndExtractJsonPath(
            """
        {
          articles(limit: 2) {
            id
            title
            url
          }
        }
      """,
            "data.articles[*]")
    assertEquals(2, data.size)
  }

  @Test
  fun `test recentArticles`() {
    val data: List<Article> =
        dgsQueryExecutor.executeAndExtractJsonPath(
            """
        {
          recentArticles {
            id
            title
            url
          }
        }
      """,
            "data.recentArticles[*]")
    assertEquals(3, data.size)
  }

  @Test
  fun `test allButRecentArticles`() {
    val data: List<Article> =
        dgsQueryExecutor.executeAndExtractJsonPath(
            """
        {
          allButRecentArticles {
            id
            title
            url
          }
        }
      """,
            "data.allButRecentArticles[*]")
    assertEquals(3, data.size)
  }

  @Test
  fun `test articlesWithNoScreenshots`() {
    val data: List<Article> =
        dgsQueryExecutor.executeAndExtractJsonPath(
            """
        {
          articlesWithNoScreenshots {
            id
            title
            url
          }
        }
      """,
            "data.articlesWithNoScreenshots[*]")
    assertEquals(3, data.size)
  }

  @Test
  fun `test tags`() {
    val data: List<String> =
        dgsQueryExecutor.executeAndExtractJsonPath(
            """
        {
          tags
        }
      """, "data.tags[*]")
    assertEquals(4, data.size)
    assertTrue(data.contains("tag1"))
    assertTrue(data.contains("tag2"))
    assertTrue(data.contains("tag3"))
    assertTrue(data.contains("tag4"))
  }
}

@Repository
@Primary
class ArticlesDataFetcherIntegrationTestsDao : DevFeedDao {

  private val articleMap: MutableMap<String, Article> = ConcurrentHashMap()
  init {
    articleMap["id1"] =
        Article(
            id = "id1",
            timestamp = 123L,
            title = "Article 1",
            url = "https://article1.url",
            tags = mutableListOf("tag1", "tag2", "tag3"))
    articleMap["id2"] =
        Article(
            id = "id2",
            timestamp = 456L,
            title = "Article 2",
            url = "https://article2.url",
            tags = mutableListOf("tag2", "tag3", "tag4"))
    articleMap["id3"] =
        Article(id = "id3", timestamp = 789L, title = "Article 3", url = "https://article3.url")
  }

  override fun existArticlesByTitleAndUrl(title: String, url: String): Boolean {
    return articleMap.filterValues { it.title == title && it.url == url }.isNotEmpty()
  }

  override fun existArticlesByUrl(url: String): Boolean {
    return articleMap.filterValues { it.url == url }.isNotEmpty()
  }

  override fun deleteByTitleAndUrl(title: String, url: String): Int {
    if (!existArticlesByTitleAndUrl(title, url)) {
      throw IllegalArgumentException("Not found: ($title, $url)")
    }
    val element =
        articleMap.remove(
            articleMap.filterValues { it.title == title && it.url == url }.map { it.key }.first())
    return if (element != null) 1 else 0
  }

  override fun existArticleParsed(url: String): Boolean {
    return articleMap.filterValues { it.url == url && it.parsed != null }.isNotEmpty()
  }

  override fun existTagByName(name: String): Boolean {
    return articleMap.flatMap { it.value.tags ?: setOf() }.contains(name)
  }

  override fun findArticleById(articleId: String): Article? {
    return articleMap.filterValues { it.id == articleId }.map { it.value }.firstOrNull()
  }

  override fun findArticleByUrl(url: String): Article? {
    return articleMap.filterValues { it.url == url }.map { it.value }.firstOrNull()
  }

  override fun insertArticle(article: Article): String {
    articleMap[article.id ?: throw IllegalArgumentException("No ID specified")] = article
    return article.id!!
  }

  override fun shouldRequestScreenshot(title: String, url: String): Boolean {
    return false
  }

  override fun shouldRequestScreenshot(articleId: String): Boolean {
    return false
  }

  override fun updateArticleScreenshotData(article: Article) {
    TODO("Not yet implemented")
  }

  override fun getArticlesWithNoScreenshots(): Collection<Article> {
    return articleMap.values.filter { it.screenshot == null }
  }

  override fun allButRecentArticles(
      limit: Int?,
      offset: Long?,
      filter: ArticleFilter?
  ): Collection<Article> {
    return if (limit != null) articleMap.values.take(limit) else articleMap.values
  }

  override fun getArticles(
      limit: Int?,
      offset: Long?,
      filter: ArticleFilter?
  ): Collection<Article> {
    return if (limit != null) articleMap.values.take(limit) else articleMap.values
  }

  override fun getArticlesDates(limit: Int?, offset: Long?): Set<Long> {
    val list = articleMap.values.map { it.timestamp }
    return (if (limit != null) list.take(limit) else list).toSet()
  }

  override fun getRecentArticles(limit: Int?, offset: Long?): Collection<Article> {
    return if (limit != null) articleMap.values.take(limit) else articleMap.values
  }

  override fun getTags(
      limit: Int?,
      offset: Long?,
      search: Collection<String>?
  ): Collection<String> {
    val list = articleMap.values.flatMap { it.tags ?: setOf() }.filterNotNull()
    return (if (limit != null) list.take(limit) else list).toSet()
  }

  override fun checkHealth() {
    TODO("Not yet implemented")
  }

  override fun close() {}
}
