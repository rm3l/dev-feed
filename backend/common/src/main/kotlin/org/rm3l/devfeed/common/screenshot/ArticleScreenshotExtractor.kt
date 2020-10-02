package org.rm3l.devfeed.common.screenshot

import org.rm3l.devfeed.common.contract.Article

interface ArticleScreenshotExtractor {

  fun extractScreenshot(article: Article)
}
