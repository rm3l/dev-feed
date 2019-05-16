package org.rm3l.tools.reverseproxy.configuration

import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ApplicationCacheConfiguration {

    private val logger = LoggerFactory.getLogger(ApplicationCacheConfiguration::class.java)

    @Value("\${proxyResponseCache.size}")
    private val proxyResponseCache: Long = 0

    @Bean(name = arrayOf("proxyResponseCacheManager"))
    fun proxyResponseCacheManager(): CacheManagerWrapper {
        val caffeineCacheManager = CaffeineCacheManager()
        caffeineCacheManager.setCaffeine(Caffeine
                .newBuilder()
                .recordStats()
                .maximumSize(this.proxyResponseCache)
                .removalListener { key, _, cause ->
                    if (logger.isDebugEnabled) {
                        logger.debug("Key $key was removed from proxyResponseCache : $cause")
                    }
                })
        return object : CacheManagerWrapper {
            override fun getCacheNames(): MutableCollection<String> {
                return caffeineCacheManager.cacheNames
            }

            override fun getCache(name: String?): Cache {
                return caffeineCacheManager.getCache(name)
            }

            override fun getCacheManagerName() = "proxyResponseCacheManager"

        }
    }
}

interface CacheManagerWrapper: CacheManager {
    fun getCacheManagerName(): String
}
