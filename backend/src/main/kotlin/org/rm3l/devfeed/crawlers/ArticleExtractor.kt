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
package org.rm3l.devfeed.crawlers

import org.rm3l.devfeed.contract.Article
import org.rm3l.devfeed.contract.ArticleParsed
import org.rm3l.devfeed.dal.DevFeedDao
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Supplier

class ArticleExtractor(private val dao: DevFeedDao, private val documentParserApiKey:String,
                       private val article: Article): Supplier<Article> {

    companion object {

        private const val ARTICLE_EXTRACTION_API_URL_FORMAT = "https://document-parser-api.lateral.io/?url=%s"

        @JvmStatic
        private val logger = LoggerFactory.getLogger(ArticleExtractor::class.java)
    }

    override fun get(): Article {
        val url = ARTICLE_EXTRACTION_API_URL_FORMAT.format(article.url)
        try {
            if (!dao.existArticleParsed(article.url)) {
                if (logger.isDebugEnabled) {
                    logger.debug("Getting article extraction data from url: $url")
                }
                val articleExtractionJsonObject =
                        khttp.get(url,
                                headers = mapOf(
                                        "Content-Type" to "application/json",
                                        "subscription-key" to documentParserApiKey
                                )).jsonObject

                val parsedImage = articleExtractionJsonObject.optString("image")

                article.parsed = ArticleParsed(
                        url = article.url,
                        title = articleExtractionJsonObject.optString("title"),
                        published = articleExtractionJsonObject.optString("published"),
                        author = articleExtractionJsonObject.optString("author"),
                        image = if (parsedImage.isNullOrBlank()) null else parsedImage,
                        description = articleExtractionJsonObject.optString("description"),
                        body = articleExtractionJsonObject.optString("body"),
                        videos = articleExtractionJsonObject.optJSONArray("videos")?.map { it.toString() }?.toSet(),
                        keywords = articleExtractionJsonObject.optJSONArray("keywords")?.map { it.toString() }?.toSet()
                )
            }
        } catch (e: Exception) {
            if (logger.isDebugEnabled) {
                logger.debug("Could not fetch article extraction data for ${article.url}: $url", e)
            }
        }
        return article
    }
}
