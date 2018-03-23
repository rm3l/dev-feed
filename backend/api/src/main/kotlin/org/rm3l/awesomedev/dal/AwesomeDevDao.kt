package org.rm3l.awesomedev.dal

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SchemaUtils.createMissingTablesAndColumns
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*

object Articles : Table(name = "articles") {
    val id = integer(name = "id").autoIncrement().primaryKey()
    val date = date(name = "date")
    val title = varchar(name = "title", length = 255)
    val description = text(name = "description").nullable()
    val link = varchar(name = "link", length = 255)
}

object Tags : Table(name = "tags") {
    val name = varchar(name = "name", length = 255).primaryKey()
}

object ArticlesTags : Table(name = "articles_tags") {
    val articleId = (integer("article_id") references Articles.id)
    val tagName = (varchar("tag_name", length = 255) references Tags.name)
}

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

    fun createDatabase() {
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

    fun insertArticleAndTags(articleDate: String, articleTitle: String, articleDescription: String?,
                             articleLink: String, tags: Set<String>?) {

        transaction {
            val articleIdentifier = Articles.insert {
                it[date] = DateTime(articleDate)
                it[title] = articleTitle
                it[description] = articleDescription
                it[link] = articleLink
            } get Articles.id

            tags?.map { articleTag ->
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

}