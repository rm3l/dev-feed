package org.rm3l.devfeed.extractors.article

import org.rm3l.devfeed.contract.Article

interface ArticleExtractor {
    fun extractArticleData(article: Article)
}
