package org.rm3l.tools.reverseproxy.configuration.auto.cache.keys

import org.springframework.cache.Cache
import org.springframework.cache.caffeine.CaffeineCache

class CaffeineCacheKeysExtractor: CacheKeysExtractor {

    override fun extractKeys(cache: Cache) = (cache as CaffeineCache).nativeCache.asMap().keys
}