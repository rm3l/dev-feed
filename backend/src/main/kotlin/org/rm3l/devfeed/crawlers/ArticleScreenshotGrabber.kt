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

import org.json.JSONObject
import org.rm3l.devfeed.contract.Article
import org.rm3l.devfeed.contract.Screenshot
import org.rm3l.devfeed.dal.DevFeedDao
import org.slf4j.LoggerFactory
import java.util.function.Supplier

/**
 * Remote Website screenshot grabber. Based upon this article:
 * https://shkspr.mobi/blog/2015/11/google-secret-screenshot-api/
 */
class ArticleScreenshotGrabber(private val dao: DevFeedDao,
                                       private val article: Article,
                                       private val updater: Boolean = false):
        Supplier<Article> {

    companion object {

        private const val GOOGLE_PAGESPEED_URL_FORMAT =
                "https://www.googleapis.com/pagespeedonline/v1/runPagespeed?url=%s&screenshot=true"

        @JvmStatic
        private val logger = LoggerFactory.getLogger(ArticleScreenshotGrabber::class.java)
    }

    override fun get(): Article {
        val url = GOOGLE_PAGESPEED_URL_FORMAT.format(article.url)
        try {
            //Check if (title, url) pair already exist in the DB
            if (updater || !dao.existArticlesByTitleAndUrl(article.title, article.url)) {
                val getRequest = khttp.get(url)
                if (getRequest.statusCode in 200..399) {
                    val screenshotJsonObject: JSONObject? =
                            getRequest.jsonObject.optJSONObject("screenshot")
                    //Weird, but for reasons best known to Google, / is replaced with _, and +
                    // is replaced with -
                    val base64ImageData = screenshotJsonObject?.optString("data")
                            ?.replace("_", "/")
                            ?.replace("-", "+")
                    val mimeType = screenshotJsonObject?.optString("mime_type")
                    val height = screenshotJsonObject?.optInt("height")
                    val width = screenshotJsonObject?.optInt("width")
                    if (!base64ImageData.isNullOrBlank()) {
                        article.screenshot = Screenshot(data = base64ImageData,
                                mimeType = mimeType,
                                width = width,
                                height = height)
                    }
                }
            }
        } catch (e: Exception) {
            if (logger.isWarnEnabled) {
                logger.warn("Could not fetch screenshot data for ${article.url}: $url", e)
            }
        }
        return article
    }

}
