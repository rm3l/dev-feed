package org.rm3l.tools.reverseproxy.configuration.auto.cache.keys

import org.springframework.cache.Cache

interface CacheKeysExtractor {

    /**
     * Get keys from a cache
     *
     * @return the cache keys
     */
    fun extractKeys(cache: Cache): Collection<*>
}