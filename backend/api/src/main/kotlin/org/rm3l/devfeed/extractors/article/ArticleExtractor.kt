package org.rm3l.devfeed.extractors.article

import org.rm3l.devfeed.common.contract.Article

interface ArticleExtractor {
    fun extractArticleData(article: Article)
}
