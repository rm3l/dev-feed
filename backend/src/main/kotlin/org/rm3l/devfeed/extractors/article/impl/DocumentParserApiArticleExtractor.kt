package org.rm3l.devfeed.extractors.article.impl

import org.rm3l.devfeed.contract.Article
import org.rm3l.devfeed.contract.ArticleParsed
import org.rm3l.devfeed.dal.DevFeedDao
import org.rm3l.devfeed.extractors.article.ArticleExtractor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["article.extraction.service"], havingValue = "document-parser-api_lateral_io", matchIfMissing = false)
class DocumentParserApiArticleExtractor : ArticleExtractor {

    @Value("\${article.extraction.service.document-parser-api_lateral_io.subscription-key}")
    private lateinit var documentParserApiKey: String

    @Autowired
    private lateinit var dao: DevFeedDao

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
        }
    }
}
