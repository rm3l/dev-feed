package org.rm3l.devfeed.articleparser.impl

import org.rm3l.devfeed.common.articleparser.ArticleExtractor
import org.rm3l.devfeed.common.contract.Article
import org.rm3l.devfeed.common.contract.ArticleParsed
import org.rm3l.devfeed.persistence.DevFeedDao
import org.slf4j.LoggerFactory

class DocumentParserApiArticleExtractor(
  private val dao: DevFeedDao,
  private val documentParserApiKey: String
): ArticleExtractor {

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
