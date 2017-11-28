package org.rm3l.iana.service_names_port_numbers.scheduling

import com.github.benmanes.caffeine.cache.LoadingCache
import org.rm3l.iana.service_names_port_numbers.configuration.ApplicationCacheConfiguration
import org.rm3l.iana.service_names_port_numbers.domain.Record
import org.rm3l.iana.service_names_port_numbers.parsers.ServiceNamePortNumberMappingParser
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ScheduledTasks(@Qualifier(value = "ianaServiceNamePortNumbersRemoteCache")
                     private val cache: LoadingCache<ServiceNamePortNumberMappingParser.Format, List<Record>>) {

    private val logger = LoggerFactory.getLogger(ScheduledTasks::class.java)

    @Scheduled(cron = "\${cacheRefresh.cron.expression}")
    fun refreshCache() {
        ServiceNamePortNumberMappingParser.Format.values()
                .forEach { format ->
                    try {
                        logger.info("[$format] Updating DB from: " +
                                ApplicationCacheConfiguration.DL_URL_FORMAT.format(format))
                        cache.refresh(format)
                        logger.info("[$format] Task scheduled. Will be refresh soon.")
                    } catch (e: Exception) {
                        //No worries
                        if (logger.isDebugEnabled) {
                            logger.debug(e.message, e)
                        }
                    }
                }
    }
}