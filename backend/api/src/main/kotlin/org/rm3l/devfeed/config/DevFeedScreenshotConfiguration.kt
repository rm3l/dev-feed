package org.rm3l.devfeed.config

import org.rm3l.devfeed.persistence.DevFeedDao
import org.rm3l.devfeed.screenshot.impl.GooglePageSpeedOnlineScreenshotExtractor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DevFeedScreenshotConfiguration {

  @Bean
  @ConditionalOnProperty(name = ["article.screenshot.service"], havingValue = "pagespeedonline", matchIfMissing = false)
  fun googlePageSpeedOnlineScreenshotExtractor(
    @Autowired devFeedDao: DevFeedDao,
    @Value("\${pagespeedonline.api.key}") pageSpeedOnlineApiKey: String,
    @Value("\${pagespeedonline.api.timeoutSeconds}") pageSpeedOnlineTimeoutSeconds: Int
  ) = GooglePageSpeedOnlineScreenshotExtractor(
    dao = devFeedDao,
    pageSpeedOnlineApiKey = pageSpeedOnlineApiKey,
    pageSpeedOnlineTimeoutSeconds = pageSpeedOnlineTimeoutSeconds)
}
