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
package org.rm3l.devfeed.persistence.impl.rdbms

import com.fasterxml.jackson.databind.ObjectMapper
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.SchemaUtils.createMissingTablesAndColumns
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.between
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.not
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.rm3l.devfeed.common.contract.Article
import org.rm3l.devfeed.common.contract.ArticleFilter
import org.rm3l.devfeed.common.contract.ArticleParsed
import org.rm3l.devfeed.common.contract.Screenshot
import org.rm3l.devfeed.common.utils.asSupportedTimestamp
import org.rm3l.devfeed.persistence.DevFeedDao
import org.slf4j.LoggerFactory
import java.net.URL
import java.sql.Connection
import java.util.UUID

private object Articles : Table(name = "articles") {
  val id = varchar(name = "id", length = 36).index()
  val timestamp = long(name = "timestamp")
  val title = text(name = "title").index()
  val description = text(name = "description").nullable()
  val link = varchar(name = "link", length = 380).index()
  val hostname = text(name = "hostname").nullable()
  val screenshotData = text(name = "screenshot_data").nullable()
  val screenshotWidth = integer(name = "screenshot_width").nullable()
  val screenshotHeight = integer(name = "screenshot_height").nullable()
  val screenshotMimeType = varchar(name = "screenshot_mime_type", length = 255).nullable()
  val articleSource = varchar(name = "source", length = 255).index()
  override val primaryKey = PrimaryKey(id, name = "article_id_pk")
}

private object Tags : Table(name = "tags") {
  val name = varchar(name = "name", length = 380).index()
  override val primaryKey = PrimaryKey(name, name = "tag_name_pk")
}

private object ArticlesTags : Table(name = "articles_tags") {
  val id = varchar(name = "id", length = 36).index()
  val articleId = varchar("article_id", length = 36).index()
  val tagName = varchar("tag_name", length = 380).index()
  override val primaryKey = PrimaryKey(id, name = "article_tags_id_pk")
}

private object ArticlesParsed : Table(name = "articles_parsed") {
  val id = varchar(name = "id", length = 36).index()
  val articleLink = varchar(name = "article_link", length = 380).index()
  val title = text(name = "title").nullable()
  val author = text(name = "author").nullable()
  val published = text(name = "published").nullable() //TODO Use DateTime
  val image = text(name = "image").nullable()
  val videos = text(name = "videos").nullable()
  val keywords = text(name = "keywords").nullable()
  val description = text(name = "description").nullable()
  val body = text(name = "body")
  override val primaryKey = PrimaryKey(id, name = "article_parsed_id_pk")
}

private const val DEFAULT_OFFSET = 0L

class DevFeedRdbmsDao(
  private val datasourceUrl: String,
  private val datasourceDriver: String,
  private val datasourceUser: String,
  private val datasourcePassword: String,
  private val datasourcePoolSize: Int = 1,
  private val objectMapper: ObjectMapper = ObjectMapper()
): DevFeedDao {

  private var datasource: HikariDataSource? = null

  companion object {
    @JvmStatic
    private val logger = LoggerFactory.getLogger(DevFeedDao::class.java)
  }

  init {
    logger.info("Database located at: '$datasourceUrl'. Driver is $datasourceDriver")

    datasource = HikariDataSource(HikariConfig().apply {
      jdbcUrl = datasourceUrl
      driverClassName = datasourceDriver
      username = datasourceUser
      password = datasourcePassword
      maximumPoolSize = datasourcePoolSize.toInt()
    })
    Database.connect(datasource!!)

    //Adjust Transaction isolation levels, as queries may not succeed in certain circumstances.
    //See https://github.com/JetBrains/Exposed/wiki/FAQ
    if ("org.sqlite.JDBC" == datasourceDriver) {
      // Or Connection.TRANSACTION_READ_UNCOMMITTED
      TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
    } else if ("oracle.jdbc.OracleDriver" == datasourceDriver) {
      // Or Connection.TRANSACTION_SERIALIZABLE
      TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_READ_COMMITTED
    }

    transaction {
      createMissingTablesAndColumns(Articles, Tags, ArticlesTags, ArticlesParsed)
    }
  }

  override fun existArticlesByTitleAndUrl(title: String, url: String): Boolean {
    var result = false
    transaction {
      result = !Articles.select { Articles.title.eq(title) and Articles.link.eq(url) }.empty()
    }
    return result
  }

  override fun existArticlesByUrl(url: String): Boolean {
    var result = false
    transaction {
      result = !Articles.select { Articles.link.eq(url) }.empty()
    }
    return result
  }

  override fun deleteByTitleAndUrl(title: String, url: String): Int {
    var result = 0
    transaction {
      result = Articles.deleteWhere { Articles.title.eq(title) and Articles.link.eq(url) }
    }
    return result
  }

  override fun existArticleParsed(url: String): Boolean {
    var result = false
    transaction {
      result = !ArticlesParsed.select { ArticlesParsed.articleLink.eq(url) }.empty()
    }
    return result
  }

  override fun existTagByName(name: String): Boolean {
    var result = false
    transaction {
      result = !Tags.select { Tags.name.eq(name) }.empty()
    }
    return result
  }

  override fun findArticleById(articleId: String): Article? {
    var result: Article? = null
    transaction {
      result = Articles.select { Articles.id.eq(articleId) }
        .limit(1)
        .map { articleResultRow ->
          val article = Article(id = articleResultRow[Articles.id],
            source = articleResultRow[Articles.articleSource],
            title = articleResultRow[Articles.title],
            url = articleResultRow[Articles.link],
            domain = articleResultRow[Articles.hostname]
              ?: URL(articleResultRow[Articles.link]).host,
            timestamp = articleResultRow[Articles.timestamp],
            screenshot = Screenshot(
              data = articleResultRow[Articles.screenshotData],
              mimeType = articleResultRow[Articles.screenshotMimeType],
              width = articleResultRow[Articles.screenshotWidth],
              height = articleResultRow[Articles.screenshotHeight]
            ),
            parsed = ArticlesParsed.select { ArticlesParsed.articleLink.eq(articleResultRow[Articles.link]) }
              .map {
                ArticleParsed(
                  url = articleResultRow[Articles.link],
                  title = it[ArticlesParsed.title],
                  description = it[ArticlesParsed.description],
                  body = it[ArticlesParsed.body],
                  author = it[ArticlesParsed.author],
                  image = if (it[ArticlesParsed.image].isNullOrBlank()) null else it[ArticlesParsed.image],
                  published = it[ArticlesParsed.published]
                  //TODO Fix videos and keywords
//                                                        videos = objectMapper.readValue(it[ArticlesParsed.videos]?:"", ArrayList::class.java)
                )
              }.firstOrNull())
          val tags = ArticlesTags.slice(ArticlesTags.tagName)
            .select { ArticlesTags.articleId.eq(articleResultRow[Articles.id]) }
            .map { it[ArticlesTags.tagName] }
            .filter { it.length >= 2 }
            .toSet()
          article.tags = tags
          article
        }
        .firstOrNull()
    }
    return result
  }

  override fun findArticleByUrl(url: String): Article? {
    var result: Article? = null
    transaction {
      result = Articles.select { Articles.link.eq(url) }
        .limit(1)
        .map { articleResultRow ->
          val article = Article(id = articleResultRow[Articles.id],
            source = articleResultRow[Articles.articleSource],
            title = articleResultRow[Articles.title],
            url = articleResultRow[Articles.link],
            domain = articleResultRow[Articles.hostname]
              ?: URL(articleResultRow[Articles.link]).host,
            timestamp = articleResultRow[Articles.timestamp],
            screenshot = Screenshot(
              data = articleResultRow[Articles.screenshotData],
              mimeType = articleResultRow[Articles.screenshotMimeType],
              width = articleResultRow[Articles.screenshotWidth],
              height = articleResultRow[Articles.screenshotHeight]
            ),
            parsed = ArticlesParsed.select { ArticlesParsed.articleLink.eq(articleResultRow[Articles.link]) }
              .map {
                ArticleParsed(
                  url = articleResultRow[Articles.link],
                  title = it[ArticlesParsed.title],
                  description = it[ArticlesParsed.description],
                  body = it[ArticlesParsed.body],
                  author = it[ArticlesParsed.author],
                  image = if (it[ArticlesParsed.image].isNullOrBlank()) null else it[ArticlesParsed.image],
                  published = it[ArticlesParsed.published]
                  //TODO Fix videos and keywords
//                                                        videos = objectMapper.readValue(it[ArticlesParsed.videos]?:"", ArrayList::class.java)
                )
              }.firstOrNull())
          val tags = ArticlesTags.slice(ArticlesTags.tagName)
            .select { ArticlesTags.articleId.eq(articleResultRow[Articles.id]) }
            .map { it[ArticlesTags.tagName] }
            .filter { it.length >= 2 }
            .toSet()
          article.tags = tags
          article
        }
        .firstOrNull()
    }
    return result
  }

  override fun insertArticle(article: Article): String {

    var articleIdentifier: String? = null
    transaction {
      articleIdentifier = Articles.insert {
        it[id] = UUID.randomUUID().toString()
        it[articleSource] = article.source!!
        it[timestamp] = article.timestamp
        it[title] = article.title
        it[description] =
          if (article.description?.length ?: 0 > 10_000)
            article.description?.substring(0 until 10_000)?.plus("...")
          else
            article.description
        it[link] = article.url
        it[hostname] = article.domain
        it[screenshotData] = article.screenshot?.data
        it[screenshotMimeType] = article.screenshot?.mimeType
        it[screenshotWidth] = article.screenshot?.width
        it[screenshotHeight] = article.screenshot?.height
      } get Articles.id

      article.parsed?.let { articleParsed ->
        ArticlesParsed.insert {
          it[articleLink] = articleParsed.url
          it[title] = articleParsed.title
          it[author] = articleParsed.author
          it[published] = articleParsed.published
          it[image] = articleParsed.image
          it[description] =
            if (articleParsed.description?.length ?: 0 > 10_000)
              articleParsed.description?.substring(0 until 10_000)?.plus("...")
            else
              articleParsed.description
          it[body] = articleParsed.body
          it[videos] = objectMapper.writeValueAsString(articleParsed.videos
            ?: emptyList<String>())
          it[keywords] = objectMapper.writeValueAsString(articleParsed.keywords
            ?: emptyList<String>())
        }
      }

      article.tags
        ?.filterNotNull()
        ?.map { articleTag -> articleTag.toLowerCase().trim().replace("\\s".toRegex(), "-") }
        ?.map { articleTag -> if (articleTag.startsWith("#")) articleTag else "#$articleTag" }
        ?.map { articleTag ->
          if (!existTagByName(articleTag)) {
            Tags.insert {
              it[name] = articleTag
            }
          }
          articleTag
        }?.forEach { tagIdInserted ->
          ArticlesTags.insert {
            it[id] = UUID.randomUUID().toString()
            it[articleId] = articleIdentifier!!
            it[tagName] = tagIdInserted
          }
        }
    }
    return articleIdentifier ?: throw IllegalStateException(
      "Could not retrieve identifier for <${article.title},${article.url}>")
  }

  override fun shouldRequestScreenshot(title: String, url: String): Boolean {
    var result = false
    transaction {
      result = Articles
        .select { Articles.title.eq(title) and Articles.link.eq(url) and Articles.screenshotData.isNotNull() and not(Articles.screenshotData eq "") }
        .empty()
    }
    return result
  }

  override fun shouldRequestScreenshot(articleId: String): Boolean {
    var result = false
    transaction {
      result = Articles
        .select { Articles.id.eq(articleId) and Articles.screenshotData.isNotNull() and not(Articles.screenshotData eq "") }
        .empty()
    }
    return result
  }

  override fun updateArticleScreenshotData(article: Article) {
    transaction {
      Articles.update({ Articles.id eq article.id!! }) {
        it[screenshotData] = article.screenshot?.data
        it[screenshotMimeType] = article.screenshot?.mimeType
        it[screenshotWidth] = article.screenshot?.width
        it[screenshotHeight] = article.screenshot?.height
      }
    }
  }

  override fun allButRecentArticles(limit: Int?, offset: Long?, filter: ArticleFilter?): Collection<Article> {
    val result = mutableListOf<Article>()
    transaction {
      Articles.slice(Articles.timestamp)
        .selectAll()
        .orderBy(Articles.timestamp, order = SortOrder.DESC)
        .withDistinct().limit(1)
        .map { it[Articles.timestamp] }
        .firstOrNull()
        ?.let { lastArticleDate ->
          val query: Query
          var whereClause = not(Op.build { Articles.timestamp.eq(lastArticleDate) })
          var tagsResolvedFromSearch: Collection<String>? = null
          if (filter != null) {
            if (filter.from != null) {
              whereClause = whereClause.and(
                Articles.timestamp.between(
                  filter.from.asSupportedTimestamp()!!,
                  filter.to?.asSupportedTimestamp()
                    ?: System.currentTimeMillis()))
            } else if (filter.to != null) {
              whereClause = whereClause.and(
                Articles.timestamp.between(System.currentTimeMillis(),
                  filter.to.asSupportedTimestamp()))
            }
            if (filter.search != null) {
              val searchPattern = "%${filter.search}%"
              val searchClause = (Articles.title like searchPattern)
                .or(Articles.description like searchPattern)
                .or(Articles.link like searchPattern)
              whereClause = whereClause.and(searchClause)
            }
            if (filter.tags != null) {
              tagsResolvedFromSearch = getTags(search = filter.tags)
            }
          }
          query = Articles.select(whereClause)
//                      limit?.let { query.limit(it, offset?:DEFAULT_OFFSET) }
          query.orderBy(Articles.timestamp, order = SortOrder.DESC)

          val list = query
            .map { articleResultRow ->
              val article = Article(id = articleResultRow[Articles.id],
                source = articleResultRow[Articles.articleSource],
                title = articleResultRow[Articles.title],
                url = articleResultRow[Articles.link],
                domain = articleResultRow[Articles.hostname]
                  ?: URL(articleResultRow[Articles.link]).host,
                timestamp = articleResultRow[Articles.timestamp],
                screenshot = Screenshot(
                  data = articleResultRow[Articles.screenshotData],
                  mimeType = articleResultRow[Articles.screenshotMimeType],
                  width = articleResultRow[Articles.screenshotWidth],
                  height = articleResultRow[Articles.screenshotHeight]
                ),
                parsed = ArticlesParsed.select { ArticlesParsed.articleLink.eq(articleResultRow[Articles.link]) }
                  .map {
                    ArticleParsed(
                      url = articleResultRow[Articles.link],
                      title = it[ArticlesParsed.title],
                      description = it[ArticlesParsed.description],
                      body = it[ArticlesParsed.body],
                      author = it[ArticlesParsed.author],
                      image = if (it[ArticlesParsed.image].isNullOrBlank()) null else it[ArticlesParsed.image],
                      published = it[ArticlesParsed.published]
                      //TODO Fix videos and keywords
//                                                        videos = objectMapper.readValue(it[ArticlesParsed.videos]?:"", ArrayList::class.java)
                    )
                  }.firstOrNull())
              val tags = ArticlesTags.slice(ArticlesTags.tagName)
                .select { ArticlesTags.articleId.eq(articleResultRow[Articles.id]) }
                .map { it[ArticlesTags.tagName] }
                .filter { it.length >= 2 }
                .toSet()
              article to tags
            }
            .filter {
              it.second.any {
                tagsResolvedFromSearch?.contains(it) ?: true
              }
            }
            .map {
              it.first.tags = it.second
              it.first
            }
            .toList()
          result.addAll(if (limit != null) list.take(limit) else list)
        }
    }
    return result
  }

  override fun getArticlesWithNoScreenshots(): Collection<Article> {
    val result = mutableListOf<Article>()
    transaction {
      result.addAll(Articles
        .select { Articles.screenshotData.isNull().or(Articles.screenshotData eq "") }
        .map { articleResultRow ->
          Article(id = articleResultRow[Articles.id],
            source = articleResultRow[Articles.articleSource],
            title = articleResultRow[Articles.title],
            url = articleResultRow[Articles.link],
            domain = articleResultRow[Articles.hostname]
              ?: URL(articleResultRow[Articles.link]).host,
            timestamp = articleResultRow[Articles.timestamp],
            screenshot = Screenshot(
              data = articleResultRow[Articles.screenshotData],
              mimeType = articleResultRow[Articles.screenshotMimeType],
              width = articleResultRow[Articles.screenshotWidth],
              height = articleResultRow[Articles.screenshotHeight]
            ),
            parsed = ArticlesParsed.select { ArticlesParsed.articleLink.eq(articleResultRow[Articles.link]) }
              .map {
                ArticleParsed(
                  url = articleResultRow[Articles.link],
                  title = it[ArticlesParsed.title],
                  description = it[ArticlesParsed.description],
                  body = it[ArticlesParsed.body],
                  author = it[ArticlesParsed.author],
                  image = if (it[ArticlesParsed.image].isNullOrBlank()) null else it[ArticlesParsed.image],
                  published = it[ArticlesParsed.published]
                  //TODO Fix videos and keywords
                  // videos = objectMapper.readValue(it[ArticlesParsed.videos]?:"",
                  //    ArrayList::class.java)
                )
              }.firstOrNull())
        }
        .toSet())
    }
    return result.toList()
  }

  override fun getArticles(limit: Int?, offset: Long?, filter: ArticleFilter?): Collection<Article> {
    val result = mutableListOf<Article>()
    transaction {
      val query: Query
      var whereClause: (SqlExpressionBuilder.() -> Op<Boolean>)? = null
      var tagsResolvedFromSearch: Collection<String>? = null
      if (filter != null) {
        val searchPattern = "%${filter.search}%"
        whereClause = {
          Articles.timestamp.between(
            filter.from?.asSupportedTimestamp() ?: 0L,
            filter.to?.asSupportedTimestamp() ?: System.currentTimeMillis())
            .and(if (filter.search != null) (Articles.title.like(searchPattern)
              .or(Articles.description like searchPattern).or(Articles.link like searchPattern)
              .or(Articles.hostname like searchPattern))
            else Articles.title.isNotNull())
            .and(if (filter.titles != null) Articles.title.inList(filter.titles!!) else Articles
              .title.isNotNull())
            .and(if (filter.urls != null) Articles.link.inList(filter.urls!!) else Articles.link
              .isNotNull())
            .and(if (filter.domains != null) Articles.hostname.inList(filter.domains!!) else
              Articles.hostname.isNotNull())
        }
        if (filter.tags != null) {
          tagsResolvedFromSearch = getTags(search = filter.tags)
        }
      }
      query = if (whereClause != null) {
        Articles.select(whereClause)
      } else {
        Articles.selectAll()
      }
//            limit?.let { query.limit(it, offset?:DEFAULT_OFFSET) }
      query.orderBy(Articles.timestamp, order = SortOrder.DESC)

      val list = query
        .map { articleResultRow ->
          val article = Article(id = articleResultRow[Articles.id],
            source = articleResultRow[Articles.articleSource],
            title = articleResultRow[Articles.title],
            url = articleResultRow[Articles.link],
            domain = articleResultRow[Articles.hostname]
              ?: URL(articleResultRow[Articles.link]).host,
            timestamp = articleResultRow[Articles.timestamp],
            screenshot = Screenshot(
              data = articleResultRow[Articles.screenshotData],
              mimeType = articleResultRow[Articles.screenshotMimeType],
              width = articleResultRow[Articles.screenshotWidth],
              height = articleResultRow[Articles.screenshotHeight]
            ),
            parsed = ArticlesParsed.select { ArticlesParsed.articleLink.eq(articleResultRow[Articles.link]) }
              .map {
                ArticleParsed(
                  url = articleResultRow[Articles.link],
                  title = it[ArticlesParsed.title],
                  description = it[ArticlesParsed.description],
                  body = it[ArticlesParsed.body],
                  author = it[ArticlesParsed.author],
                  image = if (it[ArticlesParsed.image].isNullOrBlank()) null else it[ArticlesParsed.image],
                  published = it[ArticlesParsed.published]
                  //TODO Fix videos and keywords
//                                                        videos = objectMapper.readValue(it[ArticlesParsed.videos]?:"", ArrayList::class.java)
                )
              }.firstOrNull())
          val tags = ArticlesTags.slice(ArticlesTags.tagName)
            .select { ArticlesTags.articleId.eq(articleResultRow[Articles.id]) }
            .map { it[ArticlesTags.tagName] }
            .filter { it.length >= 2 }
            .toSet()
          article to tags
        }
        .filter { it.second.any { tagsResolvedFromSearch?.contains(it) ?: true } }
        .map {
          it.first.tags = it.second
          it.first
        }
        .toList()
      result.addAll(if (limit != null) list.take(limit) else list)
    }
    return result.toList()
  }

  override fun getArticlesDates(limit: Int?, offset: Long?): Set<Long> {
    val result = mutableSetOf<Long>()
    transaction {
      val query = Articles.slice(Articles.timestamp).selectAll()
        .orderBy(Articles.timestamp, order = SortOrder.DESC)
        .withDistinct()
      limit?.let { query.limit(it, offset ?: DEFAULT_OFFSET) }
      result.addAll(query.map { it[Articles.timestamp] }.toSet())
    }
    return result.toSet()
  }

  override fun getRecentArticles(limit: Int?, offset: Long?): Collection<Article> {
    logger.trace("getRecentArticles")
    val result = mutableListOf<Article>()
    transaction {
      Articles.slice(Articles.timestamp).selectAll()
        .orderBy(Articles.timestamp, order = SortOrder.DESC)
        .limit(1)
        .withDistinct()
        .map { it[Articles.timestamp] }
        .firstOrNull()
        ?.let {
          logger.trace("getRecentArticles")
          val query = Articles.select { Articles.timestamp.eq(it) }
          limit?.let { query.limit(it, offset ?: DEFAULT_OFFSET) }
          query.orderBy(Articles.timestamp, order = SortOrder.DESC)
          result.addAll(query
            .map { articleResultRow ->
              val article = Article(id = articleResultRow[Articles.id],
                source = articleResultRow[Articles.articleSource],
                title = articleResultRow[Articles.title],
                url = articleResultRow[Articles.link],
                domain = articleResultRow[Articles.hostname]
                  ?: URL(articleResultRow[Articles.link]).host,
                timestamp = articleResultRow[Articles.timestamp],
                screenshot = Screenshot(
                  data = articleResultRow[Articles.screenshotData],
                  mimeType = articleResultRow[Articles.screenshotMimeType],
                  width = articleResultRow[Articles.screenshotWidth],
                  height = articleResultRow[Articles.screenshotHeight]
                ),
                parsed = ArticlesParsed.select { ArticlesParsed.articleLink.eq(articleResultRow[Articles.link]) }
                  .map {
                    ArticleParsed(
                      url = articleResultRow[Articles.link],
                      title = it[ArticlesParsed.title],
                      description = it[ArticlesParsed.description],
                      body = it[ArticlesParsed.body],
                      author = it[ArticlesParsed.author],
                      image = if (it[ArticlesParsed.image].isNullOrBlank()) null else it[ArticlesParsed.image],
                      published = it[ArticlesParsed.published]
                      //TODO Fix videos and keywords
//                                                        videos = objectMapper.readValue(it[ArticlesParsed.videos]?:"", ArrayList::class.java)
                    )
                  }.firstOrNull())
              val tags = ArticlesTags.slice(ArticlesTags.tagName)
                .select { ArticlesTags.articleId.eq(articleResultRow[Articles.id]) }
                .map { it[ArticlesTags.tagName] }
                .filter { it.length >= 2 }
                .toSet()
              article to tags
            }
            .map {
              it.first.tags = it.second
              it.first
            }
            .toList())
        }
    }
    return result.toList()
  }

  override fun getTags(limit: Int?, offset: Long?, search: Collection<String>?):
  Collection<String> {
    val result = mutableSetOf<String>()
    transaction {
      val tagNameSlice = Tags.slice(Tags.name)
      val query = if (search != null && search.isNotEmpty()) {
        var searchOp: Op<Boolean> = Op.TRUE
        for (tag in search) {
          searchOp = searchOp.or(Tags.name.like("%$tag%"))
        }
        tagNameSlice.select { searchOp }
      } else {
        tagNameSlice.selectAll()
      }
      query.withDistinct()
      limit?.let { query.limit(it, offset ?: DEFAULT_OFFSET) }
      result.addAll(query.map { it[Tags.name] }.filter { it.length >= 2 }.toSet())
    }
    return result.toSet()
  }

  @Throws(Exception::class)
  override fun checkHealth() {
    //Just attempt to read from the database
    transaction {
      Tags.slice(Tags.name).selectAll().limit(1)
    }
  }

  override fun close() {
    datasource?.close()
  }
}
