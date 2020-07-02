package org.rm3l.devfeed.extractors.screenshot

import org.rm3l.devfeed.contract.Article

interface ArticleScreenshotExtractor {

    fun extractScreenshot(article: Article)
}
