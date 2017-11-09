package org.rm3l.tools.reverseproxy.configuration.auto.cache.keys

import org.springframework.cache.Cache

class NoOpCacheKeysExtractor: CacheKeysExtractor {

    override fun extractKeys(cache: Cache) = emptyList<Unit>()
}