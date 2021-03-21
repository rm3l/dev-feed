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

package org.rm3l.devfeed.filters

import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.filter.CommonsRequestLoggingFilter

@Component
class DevFeedApiCommonsRequestLoggingFilter : CommonsRequestLoggingFilter() {

  @Value("\${api.request-logging.client-info}")
  private lateinit var requestLoggingClientInfo: String

  @Value("\${api.request-logging.query-string}")
  private lateinit var requestLoggingQueryString: String

  @Value("\${api.request-logging.headers}") private lateinit var requestLoggingHeaders: String

  @Value("\${api.request-logging.payload}") private lateinit var requestLoggingPayload: String

  @Value("\${api.request-logging.max-payload-length}")
  private lateinit var requestLoggingMaxPayloadLength: String

  @Value("\${api.request-logging.message-prefix}")
  private lateinit var requestLoggingMessagePrefix: String

  companion object {
    private val logger = LoggerFactory.getLogger(DevFeedApiCommonsRequestLoggingFilter::class.java)
  }

  @PostConstruct
  fun init() {
    isIncludeClientInfo = requestLoggingClientInfo.toBoolean()
    isIncludeQueryString = requestLoggingQueryString.toBoolean()
    isIncludeHeaders = requestLoggingHeaders.toBoolean()
    isIncludePayload = requestLoggingPayload.toBoolean()
    maxPayloadLength = requestLoggingMaxPayloadLength.toInt()
    this.setAfterMessagePrefix(requestLoggingMessagePrefix)
  }

  override fun shouldLog(request: HttpServletRequest) = true

  override fun beforeRequest(request: HttpServletRequest, message: String) =
      DevFeedApiCommonsRequestLoggingFilter.logger.info(message.replace("\\n", "\n"))

  override fun afterRequest(request: HttpServletRequest, message: String) =
      DevFeedApiCommonsRequestLoggingFilter.logger.info(message.replace("\\n", "\n"))
}
