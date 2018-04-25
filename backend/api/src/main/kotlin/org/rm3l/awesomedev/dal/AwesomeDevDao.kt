package org.rm3l.awesomedev.dal

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SchemaUtils.createMissingTablesAndColumns
import org.jetbrains.exposed.sql.SqlExpressionBuilder.between
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.transaction
import org.rm3l.awesomedev.crawlers.Article
import org.rm3l.awesomedev.crawlers.Screenshot
import org.rm3l.awesomedev.graphql.ArticleFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URL
import javax.annotation.PostConstruct

object Articles : Table(name = "articles") {
    val id = integer(name = "id").autoIncrement().primaryKey()
    val timestamp = long(name = "timestamp")
    val title = varchar(name = "title", length = 255)
    val description = text(name = "description").nullable()
    val link = varchar(name = "link", length = 255)
    val hostname = varchar(name = "hostname", length = 255).nullable()
    val screenshotData = text(name = "screenshot_data").nullable()
    val screenshotWidth = integer(name = "screenshot_width").nullable()
    val screenshotHeight = integer(name = "screenshot_height").nullable()
    val screenshotMimeType = varchar(name = "screenshot_mime_type", length = 255).nullable()
}

object Tags : Table(name = "tags") {
    val name = varchar(name = "name", length = 255).primaryKey()
}

object ArticlesTags : Table(name = "articles_tags") {
    val articleId = (integer("article_id") references Articles.id)
    val tagName = (varchar("tag_name", length = 255) references Tags.name)
}

const val DEFAULT_OFFSET = 0

@Component
class AwesomeDevDao {

    @Value("\${datasource.url}")
    private lateinit var datasourceUrl: String

    @Value("\${datasource.driver}")
    private lateinit var datasourceDriver: String

    @Value("\${datasource.user}")
    private lateinit var datasourceUser: String

    @Value("\${datasource.password}")
    private lateinit var datasourcePassword: String

    @PostConstruct
    fun init() {
        Database.connect(
                url = datasourceUrl,
                driver = datasourceDriver,
                user = datasourceUser,
                password = datasourcePassword)
        transaction {
            createMissingTablesAndColumns(Articles, Tags, ArticlesTags)
        }
    }

    fun existArticlesByTitleAndUrl(title: String, url: String): Boolean {
        var result = false
        transaction {
            result = !Articles.select { Articles.title.eq(title) and Articles.link.eq(url) }.empty()
        }
        return result
    }

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
                                        Articles.timestamp.between(filter.from, filter.to?: System.currentTimeMillis()))
                            } else if (filter.to != null) {
                                whereClause = whereClause.and(
                                        Articles.timestamp.between(System.currentTimeMillis(), filter.to))
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
                                .map {
                                    val article = Article(id = it[Articles.id].toLong(),
                                            title = it[Articles.title],
                                            url = it[Articles.link],
                                            domain = it[Articles.hostname] ?: URL(it[Articles.link]).host,
                                            timestamp = it[Articles.timestamp],
                                            screenshot = Screenshot(
                                                    data = it[Articles.screenshotData],
                                                    mimeType = it[Articles.screenshotMimeType],
                                                    width = it[Articles.screenshotWidth],
                                                    height = it[Articles.screenshotHeight]
                                            ))
                                    val tags = ArticlesTags.slice(ArticlesTags.tagName)
                                            .select { ArticlesTags.articleId.eq(it[Articles.id]) }
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

    fun getArticles(limit: Int? = null, offset: Int? = null, filter: ArticleFilter? = null): Collection<Article> {
        val result = mutableListOf<Article>()
        transaction {
            val query: Query
            var whereClause: (SqlExpressionBuilder.()->Op<Boolean>)? = null
            var tagsResolvedFromSearch: Collection<String>? = null
            if (filter != null) {
                val searchPattern = "%${filter.search}%"
                whereClause = {
                    Articles.timestamp.between(filter.from?: 0L, filter.to?: System.currentTimeMillis())
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
                    .map {
                        val article = Article(id = it[Articles.id].toLong(),
                                title = it[Articles.title],
                                url = it[Articles.link],
                                domain = it[Articles.hostname] ?: URL(it[Articles.link]).host,
                                timestamp = it[Articles.timestamp],
                                screenshot = Screenshot(
                                        data = it[Articles.screenshotData],
                                        mimeType = it[Articles.screenshotMimeType],
                                        width = it[Articles.screenshotWidth],
                                        height = it[Articles.screenshotHeight]
                                ))
                        val tags = ArticlesTags.slice(ArticlesTags.tagName)
                                .select { ArticlesTags.articleId.eq(it[Articles.id]) }
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
            limit?.let { query.limit(it, offset?:DEFAULT_OFFSET) }
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
                        limit?.let { query.limit(it, offset?:DEFAULT_OFFSET) }
                        query.orderBy(Articles.timestamp, isAsc = false)
                        result.addAll(query
                                .map {
                                    val article = Article(id = it[Articles.id].toLong(),
                                            title = it[Articles.title],
                                            url = it[Articles.link],
                                            domain = it[Articles.hostname] ?: URL(it[Articles.link]).host,
                                            timestamp = it[Articles.timestamp],
                                            screenshot = Screenshot(
                                                    data = it[Articles.screenshotData],
                                                    mimeType = it[Articles.screenshotMimeType],
                                                    width = it[Articles.screenshotWidth],
                                                    height = it[Articles.screenshotHeight]
                                            ))
                                    val tags = ArticlesTags.slice(ArticlesTags.tagName)
                                            .select { ArticlesTags.articleId.eq(it[Articles.id]) }
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
                    OrOpMultiple (search.map { Tags.name.like("%$it%") }.toList())
                }
            } else {
                tagNameSlice.selectAll()
            }
            query.withDistinct()
            limit?.let { query.limit(it, offset?:DEFAULT_OFFSET) }
            result.addAll(query.map { it[Tags.name] }.toSet())
        }
        return result.toSet()
    }

}

class OrOpMultiple<T>(private val expressions: Collection<Expression<T>>): Op<Boolean>() {
    override fun toSQL(queryBuilder: QueryBuilder) =
            expressions.joinToString(separator = " OR ", prefix = "(", postfix = ")") { it.toSQL(queryBuilder) }
}