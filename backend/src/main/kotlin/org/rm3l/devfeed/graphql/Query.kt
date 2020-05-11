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
package org.rm3l.devfeed.graphql

import graphql.kickstart.tools.GraphQLQueryResolver
import org.rm3l.devfeed.dal.DevFeedDao
import org.springframework.stereotype.Component

@Suppress("unused")
@Component
class Query(private val dao: DevFeedDao): GraphQLQueryResolver {

    fun articleDates(limit: Int?, offset: Int?) = dao.getArticlesDates(limit, offset)

    fun articles(limit: Int?, offset: Int?, filter: ArticleFilter?) = dao.getArticles(limit, offset, filter)

    fun recentArticles(limit: Int?, offset: Int?) = dao.getRecentArticles(limit, offset)

    fun allButRecentArticles(limit: Int?, offset: Int?, filter: ArticleFilter?) =
            dao.allButRecentArticles(limit, offset, filter)

    fun tags(limit: Int?, offset: Int?, search: List<String>?) = dao.getTags(limit, offset, search)

    fun articlesWithNoScreenshots() = dao.getArticlesWithNoScreenshots()
}
