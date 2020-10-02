package org.rm3l.devfeed.config

import org.rm3l.devfeed.articleparser.impl.DocumentParserApiArticleExtractor
import org.rm3l.devfeed.persistence.DevFeedDao
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DevFeedArticleParserConfiguration {

  @Bean
  @ConditionalOnProperty(name = ["article.extraction.service"], havingValue = "document-parser-api_lateral_io", matchIfMissing = false)
  fun documentParserApiArticleParser(
    @Value("\${article.extraction.service.document-parser-api_lateral_io.subscription-key}")
    documentParserApiKey: String,
    @Autowired dao: DevFeedDao) = DocumentParserApiArticleExtractor(dao, documentParserApiKey)
}
