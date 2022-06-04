/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Armel Soro
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

package org.rm3l.devfeed.articleparser.impl

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.runBlocking
import org.rm3l.devfeed.common.articleparser.ArticleExtractor
import org.rm3l.devfeed.common.contract.Article
import org.rm3l.devfeed.common.contract.ArticleParsed
import org.rm3l.devfeed.persistence.DevFeedDao
import org.slf4j.LoggerFactory

class DocumentParserApiArticleExtractor(
    private val dao: DevFeedDao,
    private val documentParserApiKey: String
) : ArticleExtractor {

  companion object {
    private const val ARTICLE_EXTRACTION_API_URL_FORMAT =
        "https://document-parser-api.lateral.io/?url=%s"

    @JvmStatic
    private val logger = LoggerFactory.getLogger(DocumentParserApiArticleExtractor::class.java)
  }

  override fun extractArticleData(article: Article) {
    if (documentParserApiKey.isNotBlank()) {
      val url = ARTICLE_EXTRACTION_API_URL_FORMAT.format(article.url)
      try {
        if (!dao.existArticleParsed(article.url)) {
          if (logger.isDebugEnabled) {
            logger.debug("Getting article extraction data from url: $url")
          }
          val client =
              HttpClient(Apache) {
                engine { followRedirects = true }
                install(ContentNegotiation) { jackson() }
              }
          val articleExtractionJsonObject = runBlocking {
            client
                .get(url) {
                  accept(ContentType.Application.Json)
                  header("subscription-key", documentParserApiKey)
                }
                .body<Map<String, Any>>()
          }

          val parsedImage = articleExtractionJsonObject["image"] as String?

          article.parsed =
              ArticleParsed(
                  url = article.url,
                  title = articleExtractionJsonObject["title"] as String?,
                  published = articleExtractionJsonObject["published"] as String?,
                  author = articleExtractionJsonObject["author"] as String?,
                  image = if (parsedImage.isNullOrBlank()) null else parsedImage,
                  description = articleExtractionJsonObject["description"] as String?,
                  body = articleExtractionJsonObject["body"] as String,
                  videos =
                      (articleExtractionJsonObject["videos"] as List<Any?>?)
                          ?.map { it.toString() }
                          ?.toSet(),
                  keywords =
                      (articleExtractionJsonObject["keywords"] as List<Any?>?)
                          ?.map { it.toString() }
                          ?.toSet())
        }
      } catch (e: Exception) {
        if (logger.isDebugEnabled) {
          logger.debug("Could not fetch article extraction data for ${article.url}: $url", e)
        }
      }
    }
  }
}
