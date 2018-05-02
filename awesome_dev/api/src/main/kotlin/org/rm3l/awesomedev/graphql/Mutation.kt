package org.rm3l.awesomedev.graphql

import com.coxautodev.graphql.tools.GraphQLMutationResolver
import org.rm3l.awesomedev.crawlers.Article
import org.rm3l.awesomedev.dal.AwesomeDevDao
import org.springframework.stereotype.Component

@Component
class Mutation(private val dao: AwesomeDevDao): GraphQLMutationResolver {

    fun addArticle(input: ArticleInput): Article {
        TODO("Not implemented yet")
    }

    fun deleteArticle(id: Long): Boolean {
        TODO("Not implemented yet")
    }

    fun updateArticle(id: Long, input: ArticleInput): Article {
        TODO("Not implemented yet")
    }

    fun addTag(input: String): String {
        TODO("Not implemented yet")
    }

    fun tagArticle(articleId: Long, tags: List<String>): Article {
        TODO("Not implemented yet")
    }

    fun untagArticle(articleId: Long, tagsToRemove: List<String>): Article {
        TODO("Not implemented yet")
    }
}