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

import graphql.kickstart.tools.GraphQLMutationResolver
import org.rm3l.devfeed.contract.Article
import org.rm3l.devfeed.dal.DevFeedDao
import org.springframework.stereotype.Component

@Component
class Mutation(private val dao: DevFeedDao) : GraphQLMutationResolver {

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
