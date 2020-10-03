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
