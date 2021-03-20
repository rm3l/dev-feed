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

package org.rm3l.devfeed.graphql.fetchers

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.InputArgument
import org.rm3l.devfeed.common.contract.Article
import org.rm3l.devfeed.common.contract.ArticleFilter
import org.rm3l.devfeed.common.contract.ArticleInput
import org.rm3l.devfeed.persistence.DevFeedDao
import org.springframework.beans.factory.annotation.Autowired

@DgsComponent
class ArticlesDataFetcher {

  @Autowired private lateinit var dao: DevFeedDao

  @DgsData(parentType = "Query", field = "articleDates")
  fun articleDates(@InputArgument("limit") limit: Int?, @InputArgument("offset") offset: Long?) =
      dao.getArticlesDates(limit, offset)

  @DgsData(parentType = "Query", field = "articles")
  fun articles(
      @InputArgument("limit") limit: Int?,
      @InputArgument("offset") offset: Long?,
      @InputArgument("filter") filter: ArticleFilter?
  ) = dao.getArticles(limit, offset, filter)

  @DgsData(parentType = "Query", field = "recentArticles")
  fun recentArticles(@InputArgument("limit") limit: Int?, @InputArgument("offset") offset: Long?) =
      dao.getRecentArticles(limit, offset)

  @DgsData(parentType = "Query", field = "allButRecentArticles")
  fun allButRecentArticles(
      @InputArgument("limit") limit: Int?,
      @InputArgument("offset") offset: Long?,
      @InputArgument("filter") filter: ArticleFilter?
  ) = dao.allButRecentArticles(limit, offset, filter)

  @DgsData(parentType = "Query", field = "tags")
  fun tags(
      @InputArgument("limit") limit: Int?,
      @InputArgument("offset") offset: Long?,
      @InputArgument("search") search: List<String>?
  ) = dao.getTags(limit, offset, search)

  @DgsData(parentType = "Query", field = "articlesWithNoScreenshots")
  fun articlesWithNoScreenshots() = dao.getArticlesWithNoScreenshots()

  @DgsData(parentType = "Mutation", field = "addArticle")
  fun addArticle(@InputArgument("input") input: ArticleInput): Article {
    TODO("Not implemented yet")
  }

  @DgsData(parentType = "Mutation", field = "deleteArticle")
  fun deleteArticle(@InputArgument("id") id: Long): Boolean {
    TODO("Not implemented yet")
  }

  @DgsData(parentType = "Mutation", field = "updateArticle")
  fun updateArticle(
      @InputArgument("id") id: Long,
      @InputArgument("input") input: ArticleInput
  ): Article {
    TODO("Not implemented yet")
  }

  @DgsData(parentType = "Mutation", field = "addTag")
  fun addTag(@InputArgument("input") input: String): String {
    TODO("Not implemented yet")
  }

  @DgsData(parentType = "Mutation", field = "tagArticle")
  fun tagArticle(
      @InputArgument("articleId") articleId: Long,
      @InputArgument("tags") tags: List<String>
  ): Article {
    TODO("Not implemented yet")
  }

  @DgsData(parentType = "Mutation", field = "untagArticle")
  fun untagArticle(
      @InputArgument("articleId") articleId: Long,
      @InputArgument("tagsToRemove") tagsToRemove: List<String>
  ): Article {
    TODO("Not implemented yet")
  }
}
