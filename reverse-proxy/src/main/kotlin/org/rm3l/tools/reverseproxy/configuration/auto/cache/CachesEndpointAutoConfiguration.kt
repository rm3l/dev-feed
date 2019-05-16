package org.rm3l.tools.reverseproxy.configuration.auto.cache

import org.springframework.boot.actuate.condition.ConditionalOnEnabledEndpoint
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnClass(CacheManager::class)
@ConditionalOnBean(CacheManager::class)
@AutoConfigureAfter(CacheAutoConfiguration::class)
@ConditionalOnEnabledEndpoint(ENDPOINT_NAME)
class CachesEndpointAutoConfiguration(private val cacheManagers: Map<String, CacheManager>) {

    @Bean
    fun cachesEndpoint() = CachesEndpoint(this.cacheManagers)

    @Bean
    fun cachesMvcEndpoint(cachesEndpoint: CachesEndpoint) = CachesMvcEndpoint(cachesEndpoint)
}