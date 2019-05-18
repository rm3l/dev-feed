//The MIT License (MIT)
//
//Copyright (c) 2019 Armel Soro
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to deal
//in the Software without restriction, including without limitation the rights
//to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in all
//copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//SOFTWARE.
package org.rm3l.devfeed.dal

import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SchemaUtils.createMissingTablesAndColumns
import org.jetbrains.exposed.sql.SqlExpressionBuilder.between
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.transaction
import org.rm3l.devfeed.crawlers.Article
import org.rm3l.devfeed.crawlers.ArticleParsed
import org.rm3l.devfeed.crawlers.Screenshot
import org.rm3l.devfeed.graphql.ArticleFilter
import org.rm3l.devfeed.utils.asSupportedTimestamp
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.stereotype.Component
import java.net.URL
import java.util.ArrayList
import javax.annotation.PostConstruct

object Articles : Table(name = "articles") {
    val id = integer(name = "id").autoIncrement().primaryKey()
    val timestamp = long(name = "timestamp")
    val title = text(name = "title")
    val description = text(name = "description").nullable()
    val link = varchar(name = "link", length=65535)
    val hostname = text(name = "hostname").nullable()
    val screenshotData = text(name = "screenshot_data").nullable()
    val screenshotWidth = integer(name = "screenshot_width").nullable()
    val screenshotHeight = integer(name = "screenshot_height").nullable()
    val screenshotMimeType = varchar(name = "screenshot_mime_type", length = 255).nullable()
}

object Tags : Table(name = "tags") {
    val name = varchar(name = "name", length=65535).primaryKey()
}

object ArticlesTags : Table(name = "articles_tags") {
    val articleId = (integer("article_id") references Articles.id)
    val tagName = (varchar("tag_name", length=65535) references Tags.name)
}

object ArticlesParsed : Table(name = "articles_parsed") {
    val id = integer(name = "id").autoIncrement().primaryKey()
    val url = (varchar(name = "link", length=65535) references Articles.link)
    val title = text(name = "title").nullable()
    val author = text(name = "author").nullable()
    val published = text(name = "published").nullable() //TODO Use DateTime
    val image = text(name = "image").nullable()
    val videos = text(name = "videos").nullable()
    val keywords = text(name = "keywords").nullable()
    val description = text(name = "description").nullable()
    val body = text(name = "body")
}

const val DEFAULT_OFFSET = 0

@Component
class DevFeedDao {

    @Value("\${datasource.url}")
    private lateinit var datasourceUrl: String

    @Value("\${datasource.driver}")
    private lateinit var datasourceDriver: String

    @Value("\${datasource.user}")
    private lateinit var datasourceUser: String

    @Value("\${datasource.password}")
    private lateinit var datasourcePassword: String

    @Autowired
    private lateinit var jackson2ObjectMapperBuilder: Jackson2ObjectMapperBuilder

    private lateinit var objectMapper: ObjectMapper

    @PostConstruct
    fun init() {
        this.objectMapper = jackson2ObjectMapperBuilder.build()
        Database.connect(
                url = datasourceUrl,
                driver = datasourceDriver,
                user = datasourceUser,
                password = datasourcePassword)
        transaction {
            createMissingTablesAndColumns(Articles, Tags, ArticlesTags, ArticlesParsed)
        }
    }

    @Synchronized
    fun existArticlesByTitleAndUrl(title: String, url: String): Boolean {
        var result = false
        transaction {
            result = !Articles.select { Articles.title.eq(title) and Articles.link.eq(url) }.empty()
        }
        return result
    }

    @Synchronized
    fun existArticleParsed(url: String): Boolean {
        var result = false
        transaction {
            result = !ArticlesParsed.select { ArticlesParsed.url.eq(url) }.empty()
        }
        return result
    }

    @Synchronized
    fun existTagByName(name: String): Boolean {
        var result = false
        transaction {
            result = !Tags.select { Tags.name.eq(name)}.empty()
        }
        return result
    }

    @Synchronized
    fun insertArticle(article: Article) {

        transaction {
            val articleIdentifier = Articles.insert {
                it[timestamp] = article.timestamp
                it[title] = article.title
                it[description] = article.description
                it[link] = article.url
                it[hostname] = article.domain
                it[screenshotData] = article.screenshot?.data
                it[screenshotMimeType] = article.screenshot?.mimeType
                it[screenshotWidth] = article.screenshot?.width
                it[screenshotHeight] = article.screenshot?.height
            } get Articles.id

            article.parsed?.let { articleParsed ->
                ArticlesParsed.insert {
                    it[url] = articleParsed.url
                    it[title] = articleParsed.title
                    it[author] = articleParsed.author
                    it[published] = articleParsed.published
                    it[image] = articleParsed.image
                    it[description] = articleParsed.description
                    it[body] = articleParsed.body
                    it[videos] = objectMapper.writeValueAsString(articleParsed.videos?: emptyList<String>())
                    it[keywords] = objectMapper.writeValueAsString(articleParsed.keywords?: emptyList<String>())
                }
            }

            article.tags?.map { articleTag ->
                if (!existTagByName(articleTag)) {
                    Tags.insert {
                        it[name] = articleTag
                    }
                }
                articleTag
            }?.forEach { tagIdInserted ->
                ArticlesTags.insert {
                    it[articleId] = articleIdentifier!!
                    it[tagName] = tagIdInserted
                }
            }
        }
    }

    @Synchronized
    fun updateArticleScreenshotData(article: Article) {
        transaction {
            Articles.update({ Articles.id eq article.id!!.toInt() }) {
                it[screenshotData] = article.screenshot?.data
                it[screenshotMimeType] = article.screenshot?.mimeType
                it[screenshotWidth] = article.screenshot?.width
                it[screenshotHeight] = article.screenshot?.height
            }
        }
    }

    fun allButRecentArticles(limit: Int? = null, offset: Int? = null, filter: ArticleFilter? = null): Collection<Article> {
        val result = mutableListOf<Article>()
        transaction {
            Articles.slice(Articles.timestamp)
                    .selectAll()
                    .orderBy(Articles.timestamp, isAsc = false)
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
                                                filter.to?.asSupportedTimestamp()?: System.currentTimeMillis()))
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
                        query.orderBy(Articles.timestamp, isAsc = false)

                        val list = query
                                .map {articleResultRow ->
                                    val article = Article(id = articleResultRow[Articles.id].toLong(),
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
                                            parsed = ArticlesParsed.select { ArticlesParsed.url.eq(articleResultRow[Articles.link]) }
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
                                            .toSet()
                                    article to tags
                                }
                                .filter { it.second.any { tagsResolvedFromSearch?.contains(it) ?: true } }
                                .map {
                                    it.first.tags = it.second
                                    it.first
                                }
                                .toList()
                        result.addAll(if(limit != null) list.take(limit) else list)
                    }
        }
        return result
    }

    fun getArticlesWithNoScreenshots(): Collection<Article> {
        val result = mutableListOf<Article>()
        transaction {
            result.addAll(Articles
                    .select { Articles.screenshotData.isNull().or(Articles.screenshotData eq "") }
                    .map { articleResultRow ->
                        Article(id = articleResultRow[Articles.id].toLong(),
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
                                parsed = ArticlesParsed.select { ArticlesParsed.url.eq(articleResultRow[Articles.link]) }
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
                    }
                    .toSet())
        }
        return result.toList()
    }

    fun getArticles(limit: Int? = null, offset: Int? = null, filter: ArticleFilter? = null): Collection<Article> {
        val result = mutableListOf<Article>()
        transaction {
            val query: Query
            var whereClause: (SqlExpressionBuilder.()->Op<Boolean>)? = null
            var tagsResolvedFromSearch: Collection<String>? = null
            if (filter != null) {
                val searchPattern = "%${filter.search}%"
                whereClause = {
                    Articles.timestamp.between(
                            filter.from?.asSupportedTimestamp()?: 0L,
                            filter.to?.asSupportedTimestamp()?: System.currentTimeMillis())
                            .and(if (filter.search != null) (Articles.title.like(searchPattern)
                                    .or(Articles.description like searchPattern).or(Articles.link like searchPattern))
                                else Articles.title.isNotNull())
                            .and(if (filter.titles != null) Articles.title.inList(filter.titles) else Articles.title.isNotNull())
                            .and(if (filter.urls != null) Articles.link.inList(filter.urls) else Articles.link.isNotNull())
                }
                if (filter.tags != null) {
                    tagsResolvedFromSearch = getTags(search = filter.tags)
                }
            }
            query = if (whereClause != null) { Articles.select(whereClause) } else { Articles.selectAll() }
//            limit?.let { query.limit(it, offset?:DEFAULT_OFFSET) }
            query.orderBy(Articles.timestamp, isAsc = false)

            val list = query
                    .map { articleResultRow ->
                        val article = Article(id = articleResultRow[Articles.id].toLong(),
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
                                parsed = ArticlesParsed.select { ArticlesParsed.url.eq(articleResultRow[Articles.link]) }
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
                                .toSet()
                        article to tags
                    }
                    .filter { it.second.any { tagsResolvedFromSearch?.contains(it) ?: true } }
                    .map {
                        it.first.tags = it.second
                        it.first
                    }
                    .toList()
            result.addAll(if(limit != null) list.take(limit) else list)
        }
        return result.toList()
    }

    fun getArticlesDates(limit: Int? = null, offset: Int? = null): Set<Long> {
        val result = mutableSetOf<Long>()
        transaction {
            val query = Articles.slice(Articles.timestamp).selectAll().orderBy(Articles.timestamp, isAsc = false)
                    .withDistinct()
            limit?.let { query.limit(it, offset?: DEFAULT_OFFSET) }
            result.addAll(query.map { it[Articles.timestamp] }.toSet())
        }
        return result.toSet()
    }

    fun getRecentArticles(limit: Int? = null, offset: Int? = null): Collection<Article> {
        val result = mutableListOf<Article>()
        transaction {
            Articles.slice(Articles.timestamp).selectAll().orderBy(Articles.timestamp, isAsc = false).limit(1)
                    .withDistinct()
                    .map { it[Articles.timestamp] }
                    .firstOrNull()
                    ?.let {
                        val query = Articles.select { Articles.timestamp.eq(it) }
                        limit?.let { query.limit(it, offset?: DEFAULT_OFFSET) }
                        query.orderBy(Articles.timestamp, isAsc = false)
                        result.addAll(query
                                .map {articleResultRow ->
                                    val article = Article(id = articleResultRow[Articles.id].toLong(),
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
                                            parsed = ArticlesParsed.select { ArticlesParsed.url.eq(articleResultRow[Articles.link]) }
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

    fun getTags(limit: Int? = null, offset: Int? = null, search: Collection<String>?): Collection<String> {
        val result = mutableSetOf<String>()
        transaction {
            val tagNameSlice = Tags.slice(Tags.name)
            val query = if (search != null && search.isNotEmpty()) {
                tagNameSlice.select {
                    OrOpMultiple(search.map { Tags.name.like("%$it%") }.toList())
                }
            } else {
                tagNameSlice.selectAll()
            }
            query.withDistinct()
            limit?.let { query.limit(it, offset?: DEFAULT_OFFSET) }
            result.addAll(query.map { it[Tags.name] }.toSet())
        }
        return result.toSet()
    }

}

class OrOpMultiple<T>(private val expressions: Collection<Expression<T>>): Op<Boolean>() {
    override fun toSQL(queryBuilder: QueryBuilder) =
            expressions.joinToString(separator = " OR ", prefix = "(", postfix = ")") { it.toSQL(queryBuilder) }
}
