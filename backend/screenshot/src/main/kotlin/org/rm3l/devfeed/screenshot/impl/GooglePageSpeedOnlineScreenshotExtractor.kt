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

package org.rm3l.devfeed.screenshot.impl

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.http.ContentType
import kotlinx.coroutines.runBlocking
import org.rm3l.devfeed.common.contract.Article
import org.rm3l.devfeed.common.contract.Screenshot
import org.rm3l.devfeed.common.screenshot.ArticleScreenshotExtractor
import org.rm3l.devfeed.persistence.DevFeedDao
import org.slf4j.LoggerFactory

class GooglePageSpeedOnlineScreenshotExtractor(
    private val dao: DevFeedDao,
    private val pageSpeedOnlineApiKey: String,
    private val pageSpeedOnlineTimeoutSeconds: Int = 60
) : ArticleScreenshotExtractor {

  companion object {
    private const val GOOGLE_PAGESPEED_URL_FORMAT =
        "https://www.googleapis.com/pagespeedonline/v5/runPagespeed?url=%s&screenshot=true"

    @JvmStatic
    private val logger =
        LoggerFactory.getLogger(GooglePageSpeedOnlineScreenshotExtractor::class.java)
  }

  private var googlePageSpeedOnlineApiUrlFormat: String? = null

  init {
    googlePageSpeedOnlineApiUrlFormat =
        if (pageSpeedOnlineApiKey.isBlank()) {
          GOOGLE_PAGESPEED_URL_FORMAT
        } else {
          "${GOOGLE_PAGESPEED_URL_FORMAT}&key=$pageSpeedOnlineApiKey"
        }
  }

  override fun extractScreenshot(article: Article) {
    val url = googlePageSpeedOnlineApiUrlFormat?.format(article.url) ?: GOOGLE_PAGESPEED_URL_FORMAT
    try {
      // Check if (title, url) pair already exist in the DB
      if (dao.shouldRequestScreenshot(article.title, article.url)) {
        val client =
            HttpClient(Apache) {
              engine {
                followRedirects = true
                connectTimeout = pageSpeedOnlineTimeoutSeconds
                socketTimeout = pageSpeedOnlineTimeoutSeconds
              }
              install(JsonFeature) { serializer = JacksonSerializer() }
            }
        val getRequest = runBlocking {
          client.get<Map<String, Any>>(url) { accept(ContentType.Application.Json) }
        }

        @Suppress("UNCHECKED_CAST")
        val screenshotJsonObject: Map<String, Any?>? =
            (((getRequest.get("lighthouseResult") as Map<String, Any?>?)?.get("audits") as
                        Map<String, Any?>?)
                    ?.get("final-screenshot") as
                    Map<String, Any?>?)
                ?.get("details") as
                Map<String, Any?>?
        // Weird, but for reasons best known to Google, / is replaced with _, and +
        // is replaced with -
        val base64ImageData =
            (screenshotJsonObject?.get("data") as String?)?.replace("_", "/")?.replace("-", "+")
        val mimeType = screenshotJsonObject?.get("mime_type") as String?
        val height = screenshotJsonObject?.get("height") as Int?
        val width = screenshotJsonObject?.get("width") as Int?
        if (!base64ImageData.isNullOrBlank()) {
          article.screenshot =
              Screenshot(
                  data = base64ImageData, mimeType = mimeType, width = width, height = height)
        }
      }
    } catch (e: Exception) {
      if (logger.isWarnEnabled) {
        logger.warn(
            "Could not fetch screenshot data for ${article.url}. " +
                "Exception message is: '${e.message}' " +
                "when retrieving data from the following link: $url")
      }
      if (logger.isDebugEnabled) {
        logger.debug(e.message, e)
      }
    }
  }
}
