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

package org.rm3l.devfeed.persistence.impl.mongodb

import com.mongodb.client.FindIterable
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.InsertManyOptions
import java.lang.IllegalStateException
import java.net.URL
import org.litote.kmongo.*
import org.rm3l.devfeed.common.contract.Article
import org.rm3l.devfeed.common.contract.ArticleFilter
import org.rm3l.devfeed.common.contract.ArticleParsed
import org.rm3l.devfeed.common.contract.Screenshot
import org.rm3l.devfeed.common.utils.asSupportedTimestamp
import org.rm3l.devfeed.persistence.DevFeedDao

data class ArticleDocument(
    /** Article */
    val id: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val title: String,
    val description: String? = null,
    val url: String,
    val domain: String = URL(url).host,
    var tags: Collection<String?>? = setOf(),
    var source: String? = null,

    /** Parsed */
    val parsedUrl: String? = null,
    val parsedTitle: String? = null,
    val parsedAuthor: String? = null,
    val parsedPublished: String? = null, // TODO Use DateTime
    val parsedImage: String? = null,
    val parsedVideos: Collection<String?>? = null,
    val parsedKeywords: Collection<String?>? = null,
    val parsedDescription: String? = null,
    val parsedBody: String? = null,

    /** Screenshot */
    val screenshotData: String? = null,
    val screenshotHeight: Int? = null,
    val screenshotWidth: Int? = null,
    val screenshotMimeType: String? = null
) {

  private fun hasScreenshotData(): Boolean {
    return !(screenshotData.isNullOrBlank() || screenshotMimeType.isNullOrBlank())
  }

  private fun hasParsedData(): Boolean {
    return !(parsedBody.isNullOrBlank() || parsedUrl.isNullOrBlank())
  }

  fun toArticle() =
      Article(
          id = this.id,
          timestamp = this.timestamp,
          title = this.title,
          description = this.description,
          url = this.url,
          domain = this.domain,
          tags = this.tags,
          source = this.source,
          screenshot =
              if (hasScreenshotData())
                  Screenshot(
                      data = this.screenshotData,
                      height = this.screenshotHeight,
                      width = this.screenshotWidth,
                      mimeType = this.screenshotMimeType)
              else null,
          parsed =
              if (hasParsedData())
                  ArticleParsed(
                      url = this.parsedUrl!!,
                      body = this.parsedBody!!,
                      title = this.parsedTitle,
                      author = this.parsedAuthor,
                      published = this.parsedPublished,
                      image = this.parsedImage,
                      videos = this.parsedVideos,
                      keywords = this.parsedKeywords,
                      description = this.parsedDescription)
              else null)
}

data class TagDocument(val tag: String)

class DevFeedMongoDbDao(private val connectionString: String) : DevFeedDao {

  private val mongoClient: MongoClient by lazy { KMongo.createClient(connectionString) }

  private val database: MongoDatabase by lazy { mongoClient.getDatabase("dev-feed") }

  private val articleCollection: MongoCollection<ArticleDocument> by lazy {
    database.getCollection<ArticleDocument>("articles")
  }

  private val tagCollection: MongoCollection<TagDocument> by lazy {
    database.getCollection<TagDocument>("tags")
  }

  override fun existArticlesByTitleAndUrl(title: String, url: String): Boolean {
    return articleCollection.findOne(
        ArticleDocument::title eq title, ArticleDocument::url eq url) != null
  }

  override fun existArticlesByUrl(url: String): Boolean {
    return articleCollection.findOne(ArticleDocument::url eq url) != null
  }

  override fun deleteByTitleAndUrl(title: String, url: String): Int {
    return articleCollection
        .deleteOne(ArticleDocument::title eq title, ArticleDocument::url eq url)
        .deletedCount
        .toInt()
  }

  override fun existArticleParsed(url: String): Boolean {
    return findArticleByUrl(url)?.parsed != null
  }

  override fun existTagByName(name: String): Boolean {
    return tagCollection.findOne(TagDocument::tag eq name) != null
  }

  override fun findArticleById(articleId: String): Article? {
    return articleCollection.findOne(ArticleDocument::id eq articleId)?.toArticle()
  }

  override fun findArticleByUrl(url: String): Article? {
    return articleCollection.findOne(ArticleDocument::url eq url)?.toArticle()
  }

  override fun insertArticle(article: Article): String {
    val insertedArticleResult = articleCollection.insertOne(article.toArticleDocument())
    if (!insertedArticleResult.wasAcknowledged()) {
      throw IllegalStateException("insertedId is NULL or blank for article $article")
    }
    val tagDocuments =
        article
            .tags
            ?.filterNot { it.isNullOrBlank() }
            ?.map { it!! }
            ?.map { if (it.startsWith("#")) it else "#$it" }
            ?.filterNot { existTagByName(it) }
            ?.map { TagDocument(tag = it) }
            ?.toList()
    if (!tagDocuments.isNullOrEmpty()) {
      tagCollection.insertMany(tagDocuments, InsertManyOptions().ordered(false))
    }
    return article.id!!
  }

  override fun shouldRequestScreenshot(title: String, url: String): Boolean {
    return articleCollection.findOne(ArticleDocument::title eq title, ArticleDocument::url eq url)
        ?.screenshotData == null
  }

  override fun shouldRequestScreenshot(articleId: String): Boolean {
    return findArticleById(articleId)?.screenshot == null
  }

  override fun updateArticleScreenshotData(article: Article) {
    articleCollection.updateOne(
        ArticleDocument::id eq article.id,
        set(
            SetTo(ArticleDocument::screenshotData, article.screenshot?.data),
            SetTo(ArticleDocument::screenshotMimeType, article.screenshot?.mimeType),
            SetTo(ArticleDocument::screenshotWidth, article.screenshot?.width),
            SetTo(ArticleDocument::screenshotHeight, article.screenshot?.height)))
  }

  override fun getArticlesWithNoScreenshots(): Collection<Article> {
    return articleCollection
        .find()
        .filter { it.screenshotData == null }
        .map { it.toArticle() }
        .sortedBy { it.title }
  }

  override fun allButRecentArticles(
      limit: Int?,
      offset: Long?,
      filter: ArticleFilter?
  ): Collection<Article> {
    val recentArticlesIds = this.getRecentArticles(limit, offset).map { it.id }.toSet()
    return getArticles(limit, offset, filter)
        .filterNot { recentArticlesIds.contains(it.id) }
        .sortedBy { it.title }
  }

  override fun getArticles(
      limit: Int?,
      offset: Long?,
      filter: ArticleFilter?
  ): Collection<Article> {
    var findIterable: FindIterable<ArticleDocument>
    val filtersAsBsonList = filter?.toBsonFilters()
    if (filter == null || filtersAsBsonList == null) {
      findIterable =
          if (limit == null) {
            if (offset == null) {
              articleCollection.find()
            } else {
              articleCollection.find().skip(offset.toInt())
            }
          } else {
            if (offset == null) {
              articleCollection.find().limit(limit.toInt())
            } else {
              articleCollection.find().skip(offset.toInt()).limit(limit.toInt())
            }
          }
    } else {
      if (filtersAsBsonList.isEmpty()) {
        if (filter.from == null && filter.to == null) {
          return listOf()
        }
        findIterable = articleCollection.find()
      } else {
        val filterToBson = Filters.and(filtersAsBsonList)
        findIterable =
            if (limit == null) {
              if (offset == null) {
                articleCollection.find(filterToBson)
              } else {
                articleCollection.find(filterToBson).skip(offset.toInt())
              }
            } else {
              if (offset == null) {
                articleCollection.find(filterToBson).limit(limit.toInt())
              } else {
                articleCollection.find(filterToBson).skip(offset.toInt()).limit(limit.toInt())
              }
            }
      }
    }
    val articles = findIterable.map { it.toArticle() }.toList()
    val articlesFiltered =
        if (filter?.from != null) {
          val fromTimestamp = filter.from.asSupportedTimestamp()!!
          if (filter.to != null) {
            val toTimestamp = filter.to.asSupportedTimestamp()!!
            articles
                .filter { it.timestamp >= fromTimestamp && it.timestamp <= toTimestamp }
                .toList()
          } else {
            articles.filter { it.timestamp >= fromTimestamp }.toList()
          }
        } else {
          if (filter?.to != null) {
            val toTimestamp = filter.to.asSupportedTimestamp()!!
            articles.filter { it.timestamp <= toTimestamp }.toList()
          } else {
            articles
          }
        }
    return articlesFiltered.sortedBy { it.title }
  }

  override fun getArticlesDates(limit: Int?, offset: Long?): Set<Long> {
    return getArticles(limit, offset).map { it.timestamp }.sorted().toSet()
  }

  override fun getRecentArticles(limit: Int?, offset: Long?): Collection<Article> {
    val currentTimestamp = System.currentTimeMillis()
    val articlesByTimestampDesc =
        getArticles(limit, offset)
            .filter {
              // #198 : Filter out articles that have a timestamp in the future
              it.timestamp <= currentTimestamp
            }
            .sortedByDescending { it.timestamp }
    if (articlesByTimestampDesc.isEmpty()) {
      return listOf()
    }
    val firstTimestamp = articlesByTimestampDesc[0].timestamp
    return articlesByTimestampDesc.filter { it.timestamp == firstTimestamp }.sortedBy { it.title }
  }

  override fun getTags(
      limit: Int?,
      offset: Long?,
      search: Collection<String>?
  ): Collection<String> {

    val findIterable: FindIterable<TagDocument>
    if (search == null) {
      findIterable =
          if (limit == null) {
            if (offset == null) {
              tagCollection.find()
            } else {
              tagCollection.find().skip(offset.toInt())
            }
          } else {
            if (offset == null) {
              tagCollection.find().limit(limit.toInt())
            } else {
              tagCollection.find().skip(offset.toInt()).limit(limit.toInt())
            }
          }
    } else {
      val filterToBson = Filters.or(search.map { TagDocument::tag eq it }.toList())
      findIterable =
          if (limit == null) {
            if (offset == null) {
              tagCollection.find(filterToBson)
            } else {
              tagCollection.find(filterToBson).skip(offset.toInt())
            }
          } else {
            if (offset == null) {
              tagCollection.find(filterToBson).limit(limit.toInt())
            } else {
              tagCollection.find(filterToBson).skip(offset.toInt()).limit(limit.toInt())
            }
          }
    }
    return findIterable
        .map { it.tag }
        .filter {
          // At least '#<something>'
          it.length >= 2
        }
        .filter { search?.contains(it) ?: true }
        .toSet()
        .sorted()
  }

  override fun checkHealth() {
    // Just attempt to read databases
    mongoClient.listDatabaseNames()
  }

  override fun close() {
    mongoClient.close()
  }
}
