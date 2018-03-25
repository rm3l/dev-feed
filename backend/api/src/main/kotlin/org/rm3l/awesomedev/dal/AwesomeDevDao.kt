package org.rm3l.awesomedev.dal

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SchemaUtils.createMissingTablesAndColumns
import org.jetbrains.exposed.sql.SqlExpressionBuilder.between
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.rm3l.awesomedev.crawlers.Article
import org.rm3l.awesomedev.graphql.ArticleFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URL
import javax.annotation.PostConstruct

object Articles : Table(name = "articles") {
    val id = integer(name = "id").autoIncrement().primaryKey()
    val date = date(name = "date")
    val title = varchar(name = "title", length = 255)
    val description = text(name = "description").nullable()
    val link = varchar(name = "link", length = 255)
    val hostname = varchar(name = "hostname", length = 255).nullable()
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
    fun insertArticleAndTags(article: Article) {

        transaction {
            val articleIdentifier = Articles.insert {
                it[date] = DateTime(article.date)
                it[title] = article.title
                it[description] = article.description
                it[link] = article.url
                it[hostname] = article.domain
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

    fun getArticles(limit: Int? = null, offset: Int? = null, filter: ArticleFilter? = null): Collection<Article> {
        val result = mutableListOf<Article>()
        transaction {
            val query: Query
            var whereClause: Op<Boolean>? = null
            var tagsResolvedFromSearch: Collection<String>? = null
            if (filter != null) {
                if (filter.from != null) {
                    whereClause = Articles.date.between(DateTime(filter.from),
                            filter.to?.map { DateTime(it) } ?: DateTime.now())
                } else if (filter.to != null) {
                    whereClause = Articles.date.between(DateTime.now(), DateTime(filter.to))
                }
                if (filter.search != null) {
                    val searchPattern = "%${filter.search}%"
                    val searchClause = (Articles.title like searchPattern)
                            .or(Articles.description like searchPattern)
                            .or(Articles.link like searchPattern)
                    whereClause = whereClause?.and(searchClause)?:searchClause
                }
                if (filter.tags != null) {
                    tagsResolvedFromSearch = getTags(search = filter.tags)
                }
            }
            query = if (whereClause != null) Articles.select(whereClause) else Articles.selectAll()
            limit?.let { query.limit(it, offset?:DEFAULT_OFFSET) }
            query.orderBy(Articles.date, isAsc = false)

            result.addAll(query
                    .map {
                        val article = Article(id = it[Articles.id].toLong(),
                                title = it[Articles.title],
                                url = it[Articles.link],
                                domain = it[Articles.hostname] ?: URL(it[Articles.link]).host,
                                date = it[Articles.date].toDate().toString())
                        val tags = ArticlesTags.slice(ArticlesTags.tagName)
                                .select { ArticlesTags.articleId.eq(it[Articles.id]) }
                                .map { it[ArticlesTags.tagName] }
                                .toSet()
                        article to tags
                    }
                    .filter { it.second.any { tagsResolvedFromSearch?.contains(it)?:true } }
                    .map {
                        it.first.tags = it.second
                        it.first
                    }
                    .toList())
        }
        return result.toList()
    }

    fun getArticlesDates(limit: Int? = null, offset: Int? = null): Set<String> {
        val result = mutableSetOf<String>()
        transaction {
            val query = Articles.slice(Articles.date).selectAll().orderBy(Articles.date, isAsc = false)
                    .withDistinct()
            limit?.let { query.limit(it, offset?:DEFAULT_OFFSET) }
            result.addAll(
                    query.map { it[Articles.date].toDate().toString() }
                            .toSet())
        }
        return result.toSet()
    }

    fun getRecentArticles(limit: Int? = null, offset: Int? = null): Collection<Article> {
        val result = mutableListOf<Article>()
        transaction {
            Articles.slice(Articles.date).selectAll().orderBy(Articles.date, isAsc = false).limit(1)
                    .withDistinct()
                    .map { it[Articles.date] }
                    .firstOrNull()
                    ?.let {
                        val query = Articles.select { Articles.date.eq(it) }
                        limit?.let { query.limit(it, offset?:DEFAULT_OFFSET) }
                        query.orderBy(Articles.date, isAsc = false)
                        result.addAll(query
                                .map {
                                    val article = Article(id = it[Articles.id].toLong(),
                                            title = it[Articles.title],
                                            url = it[Articles.link],
                                            domain = it[Articles.hostname] ?: URL(it[Articles.link]).host,
                                            date = it[Articles.date].toDate().toString())
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