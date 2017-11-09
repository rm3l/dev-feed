package org.rm3l.tools.reverseproxy.configuration.auto.cache.keys

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.cache.ehcache.EhCacheCache
import kotlin.reflect.KClass


class CacheKeysExtractorFactory private constructor(/*Avoid instantiation*/) {

    companion object {
        private val KEYS_EXTRACTORS_BY_CACHE_CLASS = ConcurrentHashMap<KClass<*>, CacheKeysExtractor>()

        fun getKeyExtractor(cache: Cache) =
                KEYS_EXTRACTORS_BY_CACHE_CLASS
                        .computeIfAbsent(cache::class) { _ -> createCacheKeysExtractor(cache) }

        private fun createCacheKeysExtractor(cache: Cache): CacheKeysExtractor {
            return when (cache) {
                is CaffeineCache -> CaffeineCacheKeysExtractor()
                is EhCacheCache -> TODO("Not implemented as yet")
                is ConcurrentMapCache -> TODO("Not implemented as yet")
                else -> NoOpCacheKeysExtractor()
            }
        }
    }
}