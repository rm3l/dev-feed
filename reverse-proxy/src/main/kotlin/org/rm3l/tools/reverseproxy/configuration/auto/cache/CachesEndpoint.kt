package org.rm3l.tools.reverseproxy.configuration.auto.cache

import org.springframework.boot.actuate.endpoint.AbstractEndpoint
import org.springframework.cache.CacheManager

const val ENDPOINT_NAME = "caches"
class CachesEndpoint(private val cacheManagers: Map<String, CacheManager>):
        AbstractEndpoint<Map<String, Collection<String>>>(ENDPOINT_NAME) {

    override fun invoke() = cacheManagers.entries.map { it.key to it.value.cacheNames }.toMap()

    fun getCacheManagerByName(cacheManager: String) =  cacheManagers[cacheManager]
}