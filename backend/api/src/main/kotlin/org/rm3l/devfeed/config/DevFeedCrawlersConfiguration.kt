/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Armel Soro
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.rm3l.devfeed.config

import java.util.concurrent.ExecutorService
import org.rm3l.devfeed.crawlers.DummyCrawler
import org.rm3l.devfeed.crawlers.discoverdev_io.DiscoverDevIoCrawler
import org.rm3l.devfeed.crawlers.engineeringblogs_xyz.EngineeringBlogsCrawler
import org.rm3l.devfeed.crawlers.rm3l_org.Rm3lOrgCrawler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DevFeedCrawlersConfiguration {

  @Autowired
  @Qualifier("devFeedExecutorService")
  private lateinit var devFeedExecutorService: ExecutorService

  @Bean
  @ConditionalOnProperty(
      name = ["crawlers.dummy.enabled"], havingValue = "true", matchIfMissing = false)
  fun dummyCrawler() = DummyCrawler()

  @Bean
  @ConditionalOnProperty(
      name = ["crawlers.discoverdev_io.enabled"], havingValue = "true", matchIfMissing = true)
  fun discoverDevIoCrawler() = DiscoverDevIoCrawler(devFeedExecutorService)

  @Bean
  @ConditionalOnProperty(
      name = ["crawlers.engineeringblogs_xyz.enabled"], havingValue = "true", matchIfMissing = true)
  fun engineeringBlogsXyzCrawler() = EngineeringBlogsCrawler(devFeedExecutorService)

  @Bean
  @ConditionalOnProperty(
      name = ["crawlers.rm3l_org.enabled"], havingValue = "true", matchIfMissing = true)
  fun rm3lOrgCrawler() = Rm3lOrgCrawler()
}
