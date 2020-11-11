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

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.CommonsRequestLoggingFilter
import javax.servlet.http.HttpServletRequest

@Configuration
class DevFeedApiConfiguration {

  @Value("\${api.request-logging.client-info}")
  private lateinit var requestLoggingClientInfo: String

  @Value("\${api.request-logging.query-string}")
  private lateinit var requestLoggingQueryString: String

  @Value("\${api.request-logging.headers}")
  private lateinit var requestLoggingHeaders: String

  @Value("\${api.request-logging.payload}")
  private lateinit var requestLoggingPayload: String

  @Value("\${api.request-logging.max-payload-length}")
  private lateinit var requestLoggingMaxPayloadLength: String

  @Value("\${api.request-logging.message-prefix}")
  private lateinit var requestLoggingMessagePrefix: String

  @Bean
  @ConditionalOnProperty(
    name = ["api.request-logging.enabled"],
    havingValue = "true",
    matchIfMissing = true
  )
  fun requestLoggingFilter() = DevFeedApiCommonsRequestLoggingFilter().apply {
    setIncludeClientInfo(requestLoggingClientInfo.toBoolean())
    setIncludeQueryString(requestLoggingQueryString.toBoolean())
    setIncludeHeaders(requestLoggingHeaders.toBoolean())
    setIncludePayload(requestLoggingPayload.toBoolean())
    setMaxPayloadLength(requestLoggingMaxPayloadLength.toInt())
    setAfterMessagePrefix(requestLoggingMessagePrefix)
  }
}

class DevFeedApiCommonsRequestLoggingFilter : CommonsRequestLoggingFilter() {
  override fun shouldLog(request: HttpServletRequest): Boolean {
    return true
  }
}
