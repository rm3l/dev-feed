package org.rm3l.devfeed.config

import org.rm3l.devfeed.crawlers.discoverdev_io.DiscoverDevIoCrawler
import org.rm3l.devfeed.crawlers.engineeringblogs_xyz.EngineeringBlogsCrawler
import org.rm3l.devfeed.crawlers.rm3l_org.Rm3lOrgCrawler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.ExecutorService

@Configuration
class DevFeedCrawlersConfiguration {

  @Autowired
  @Qualifier("devFeedExecutorService")
  private lateinit var devFeedExecutorService: ExecutorService

  @Bean
  @ConditionalOnProperty(name = ["crawlers.discoverdev_io.enabled"], havingValue = "true", matchIfMissing = true)
  fun discoverDevIoCrawler() = DiscoverDevIoCrawler(devFeedExecutorService)

  @Bean
  @ConditionalOnProperty(name = ["crawlers.engineeringblogx_xyz.enabled"], havingValue = "true", matchIfMissing = true)
  fun engineeringBlogsXyzCrawler() = EngineeringBlogsCrawler(devFeedExecutorService)

  @Bean
  @ConditionalOnProperty(name = ["crawlers.rm3l_org.enabled"], havingValue = "true", matchIfMissing = true)
  fun rm3lOrgCrawler() = Rm3lOrgCrawler()
}
