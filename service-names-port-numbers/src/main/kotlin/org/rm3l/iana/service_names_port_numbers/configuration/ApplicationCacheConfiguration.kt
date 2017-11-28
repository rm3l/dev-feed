package org.rm3l.iana.service_names_port_numbers.configuration

import com.github.benmanes.caffeine.cache.Caffeine
import org.rm3l.iana.service_names_port_numbers.domain.Record
import org.rm3l.iana.service_names_port_numbers.parsers.ServiceNamePortNumberMappingParser
import org.slf4j.LoggerFactory
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import java.util.concurrent.TimeUnit

@Configuration
@EnableScheduling
class ApplicationCacheConfiguration(restTemplateBuilder: RestTemplateBuilder) : ApplicationContextAware {

    private var applicationContext: ApplicationContext? = null

    override fun setApplicationContext(applicationContext: ApplicationContext?) {
        this.applicationContext = applicationContext
    }

    private val logger = LoggerFactory.getLogger(ApplicationCacheConfiguration::class.java)

    private val restTemplate = restTemplateBuilder.build()

    @Bean
    fun ianaServiceNamePortNumbersRemoteCache() = Caffeine
            .newBuilder()
            .recordStats()
            .maximumSize(ServiceNamePortNumberMappingParser.Format.values().size.toLong())
            .expireAfterWrite(1L, TimeUnit.DAYS)
            .expireAfterAccess(1L, TimeUnit.DAYS)
            .removalListener<ServiceNamePortNumberMappingParser.Format, List<Record>> { key, _, cause ->
                if (logger.isDebugEnabled) {
                    logger.debug("Key $key was removed from cache : $cause")
                }
            }
            .build<ServiceNamePortNumberMappingParser.Format, List<Record>> { format ->
                val content = restTemplate.getForObject(DL_URL_FORMAT.format(format.name.toLowerCase()),
                        String::class.java)
                val parser = this.applicationContext?.getBean(format.name.toLowerCase(),
                        ServiceNamePortNumberMappingParser::class.java) ?: TODO("Not implemented as yet: $format")
                parser.parse(content)
            }

    companion object {
        @JvmField
        val DL_URL_FORMAT =
                "https://www.iana.org/assignments/service-names-port-numbers/service-names-port-numbers.%s"
    }
}
