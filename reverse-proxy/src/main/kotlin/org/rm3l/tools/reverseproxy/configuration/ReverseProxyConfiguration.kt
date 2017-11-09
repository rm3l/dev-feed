package org.rm3l.tools.reverseproxy.configuration

import com.fasterxml.jackson.module.kotlin.KotlinModule
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

    //Will be auto-added by SpringBoot to ObjectMapperBuilder component
    @Bean
    fun jacksonKotlinModule() = KotlinModule()
}