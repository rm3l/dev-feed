package org.rm3l.awesomedev.graphql

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import org.rm3l.awesomedev.crawlers.Article
import org.rm3l.awesomedev.dal.AwesomeDevDao
import org.springframework.stereotype.Component

@Component
class Query(private val dao: AwesomeDevDao): GraphQLQueryResolver {

    fun articleDates(limit: Int?, offset: Int?) = dao.getArticlesDates(limit, offset)

    fun articles(limit: Int?, offset: Int?, filter: ArticleFilter?) = dao.getArticles(limit, offset, filter)

    fun recentArticles(limit: Int?, offset: Int?) = dao.getRecentArticles(limit, offset)

    fun allButRecentArticles(limit: Int?, offset: Int?, filter: ArticleFilter?) =
            dao.allButRecentArticles(limit, offset, filter)

    fun tags(limit: Int?, offset: Int?, search: List<String>?) = dao.getTags(limit, offset, search)
}
