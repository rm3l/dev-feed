package org.rm3l.awesomedev.graphql

import com.coxautodev.graphql.tools.GraphQLMutationResolver
import org.rm3l.awesomedev.crawlers.Article
import org.rm3l.awesomedev.dal.AwesomeDevDao
import org.springframework.stereotype.Component

@Component
class Mutation(private val dao: AwesomeDevDao): GraphQLMutationResolver {

    fun addArticle(input: ArticleInput): Article {
        TODO()
    }

    fun deleteArticle(id: Long): Boolean {
        TODO()
    }

    fun updateArticle(id: Long, input: ArticleInput): Article {
        TODO()
    }

    fun addTag(input: String): String {
        TODO()
    }

    fun tagArticle(articleId: Long, tags: List<String>): Article {
        TODO()
    }

    fun untagArticle(articleId: Long, tagsToRemove: List<String>): Article {
        TODO()
    }
}