package org.rm3l.tools.reverseproxy.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.CommonsRequestLoggingFilter

@Configuration
class ReverseProxyConfiguration {

    @Bean
    fun requestLoggingFilter(): CommonsRequestLoggingFilter {
        val loggingFilter = CommonsRequestLoggingFilter()
        loggingFilter.setIncludeClientInfo(true)
        loggingFilter.isIncludeHeaders = true
        loggingFilter.setIncludeQueryString(true)
        loggingFilter.setIncludePayload(true)
        return loggingFilter
    }
}