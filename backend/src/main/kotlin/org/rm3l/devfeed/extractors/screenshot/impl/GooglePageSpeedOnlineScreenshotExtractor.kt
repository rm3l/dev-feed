package org.rm3l.devfeed.extractors.screenshot.impl

import org.json.JSONObject
import org.rm3l.devfeed.contract.Article
import org.rm3l.devfeed.contract.Screenshot
import org.rm3l.devfeed.dal.DevFeedDao
import org.rm3l.devfeed.extractors.screenshot.ArticleScreenshotExtractor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

/**
 * Remote Website screenshot grabber. Based upon this article:
 * https://shkspr.mobi/blog/2015/11/google-secret-screenshot-api/
 */
@Service
@ConditionalOnProperty(name = ["article.screenshot.service"], havingValue = "pagespeedonline", matchIfMissing = false)
class GooglePageSpeedOnlineScreenshotExtractor(private val dao: DevFeedDao) : ArticleScreenshotExtractor {

    companion object {
        private const val GOOGLE_PAGESPEED_URL_FORMAT =
                "https://www.googleapis.com/pagespeedonline/v5/runPagespeed?url=%s&screenshot=true"

        @JvmStatic
        private val logger = LoggerFactory.getLogger(GooglePageSpeedOnlineScreenshotExtractor::class.java)
    }

    @Value("\${pagespeedonline.api.key}")
    private lateinit var pageSpeedOnlineApiKey: String

    @Value("\${pagespeedonline.api.timeoutSeconds}")
    private lateinit var pageSpeedOnlineTimeoutSeconds: String

    private var googlePageSpeedOnlineApiUrlFormat: String? = null

    @PostConstruct
    fun init() {
        googlePageSpeedOnlineApiUrlFormat = if (pageSpeedOnlineApiKey.isBlank()) {
            GOOGLE_PAGESPEED_URL_FORMAT
        } else {
            "${GOOGLE_PAGESPEED_URL_FORMAT}&key=$pageSpeedOnlineApiKey"
        }
    }

    override fun extractScreenshot(article: Article) {
        val url = googlePageSpeedOnlineApiUrlFormat?.format(article.url)
                ?: GOOGLE_PAGESPEED_URL_FORMAT
        try {
            //Check if (title, url) pair already exist in the DB
            if (dao.shouldRequestScreenshot(article.title, article.url)) {
                val getRequest = khttp.get(url,
                        timeout = pageSpeedOnlineTimeoutSeconds.toDouble())
                if (getRequest.statusCode in 200..399) {
                    val screenshotJsonObject: JSONObject? =
                            getRequest.jsonObject.optJSONObject("lighthouseResult")
                                    .optJSONObject("audits")
                                    .optJSONObject("final-screenshot")
                                    .optJSONObject("details")
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
