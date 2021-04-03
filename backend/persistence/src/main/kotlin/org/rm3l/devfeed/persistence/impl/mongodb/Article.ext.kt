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

package org.rm3l.devfeed.persistence.impl.mongodb

import com.mongodb.client.model.Filters
import org.bson.conversions.Bson
import org.litote.kmongo.contains
import org.litote.kmongo.eq
import org.litote.kmongo.regex
import org.rm3l.devfeed.common.contract.Article
import org.rm3l.devfeed.common.contract.ArticleFilter

fun Article.toArticleDocument(): ArticleDocument {
  return ArticleDocument(
      id = this.id,
      timestamp = this.timestamp,
      title = this.title,
      description = this.description,
      url = this.url,
      domain = this.domain,
      tags = this.tags?.map { if (it?.startsWith("#") == true) it else "#$it" }?.toSet(),
      screenshotData = this.screenshot?.data,
      screenshotMimeType = this.screenshot?.mimeType,
      screenshotWidth = this.screenshot?.width,
      screenshotHeight = this.screenshot?.height,
      parsedUrl = this.parsed?.url,
      parsedTitle = this.parsed?.title,
      parsedAuthor = this.parsed?.author,
      parsedPublished = this.parsed?.published,
      parsedImage = this.parsed?.image,
      parsedVideos = this.parsed?.videos,
      parsedKeywords = this.parsed?.keywords,
      parsedDescription = this.parsed?.description,
      parsedBody = this.parsed?.body,
      source = this.source)
}

fun ArticleFilter?.toBsonFilters(): List<Bson>? {
  if (this == null) {
    return null
  }
  val mutableFilterList = mutableListOf<Bson>()
  if (!this.domains.isNullOrEmpty()) {
    mutableFilterList.add(
        Filters.or(
            this.domains?.map { ArticleDocument::domain regex it.toRegex() }?.toList() ?: listOf()))
  }
  if (!this.titles.isNullOrEmpty()) {
    mutableFilterList.add(
        Filters.or(
            this.titles?.map { ArticleDocument::title regex it.toRegex() }?.toList() ?: listOf()))
  }
  if (!this.urls.isNullOrEmpty()) {
    mutableFilterList.add(
        Filters.or(this.urls?.map { ArticleDocument::url eq it }?.toList() ?: listOf()))
  }
  if (!this.tags.isNullOrEmpty()) {
    mutableFilterList.add(
        Filters.or(
            this.tags!!
                .map { if (it.startsWith("#") == true) it else "#$it" }
                .map { ArticleDocument::tags contains it }
                .toList()))
  }
  if (this.search != null) {
    mutableFilterList.add(
        Filters.or(
            ArticleDocument::title regex this.search!!.toRegex(),
            ArticleDocument::description regex this.search!!.toRegex(),
            ArticleDocument::url regex this.search!!.toRegex(),
            ArticleDocument::domain regex this.search!!.toRegex()))
  }
  return mutableFilterList.toList()
}
