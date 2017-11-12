package org.rm3l.tools.reverseproxy.configuration.auto.cache

import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.endpoint.mvc.ActuatorMediaTypes
import org.springframework.boot.actuate.endpoint.mvc.EndpointMvcAdapter
import org.springframework.boot.actuate.endpoint.mvc.HypermediaDisabled
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.rm3l.tools.reverseproxy.configuration.auto.cache.keys.CacheKeysExtractorFactory

@ConfigurationProperties(prefix = "endpoints.caches")
class CachesMvcEndpoint(private val cachesEndpoint: CachesEndpoint): EndpointMvcAdapter(cachesEndpoint) {

    private val logger = LoggerFactory.getLogger(CachesMvcEndpoint::class.java)

    @RequestMapping(value = "/{cacheManager:.*}",
            method = arrayOf(RequestMethod.GET),
            produces = arrayOf(ActuatorMediaTypes.APPLICATION_ACTUATOR_V1_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE))
    @HypermediaDisabled
    fun getCachesByCacheManager(@PathVariable("cacheManager") cacheManagerName: String):
            ResponseEntity<Collection<String>> {

        if (!cachesEndpoint.isEnabled) {
            return ResponseEntity.notFound().build()
        }

        val cacheManager = cachesEndpoint.getCacheManagerByName(cacheManagerName) ?:
                return ResponseEntity.notFound().build()

        return ResponseEntity.ok(cacheManager.cacheNames)
    }

    @DeleteMapping("/{cacheManager:.*}/{cacheName:.*}")
    @HypermediaDisabled
    fun clearCache(@PathVariable("cacheManager") cacheManagerName: String,
                   @PathVariable("cacheName") cacheName: String): ResponseEntity<Void> {

        if (!cachesEndpoint.isEnabled) {
            return ResponseEntity.notFound().build()
        }

        val cacheManager = cachesEndpoint.getCacheManagerByName(cacheManagerName) ?:
                return ResponseEntity.notFound().build()

        if (!cacheManager.cacheNames.contains(cacheName)) {
            return ResponseEntity.notFound().build()
        }

        val cache = cacheManager.getCache(cacheName)

        cache.clear()
        logger.info("Cache {} from CacheManager {} cleared", cacheName, cacheManagerName)

        return ResponseEntity.noContent().build()

    }

    @RequestMapping(value = "/{cacheManager:.*}/{cacheName:.*}/keys",
            method = arrayOf(RequestMethod.GET),
            produces = arrayOf(ActuatorMediaTypes.APPLICATION_ACTUATOR_V1_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE))
    @HypermediaDisabled
    fun getCacheKeys(@PathVariable("cacheManager") cacheManagerName: String,
                     @PathVariable("cacheName") cacheName: String): ResponseEntity<Collection<*>> {

        if (!cachesEndpoint.isEnabled) {
            return ResponseEntity.notFound().build()
        }

        val cacheManager = cachesEndpoint.getCacheManagerByName(cacheManagerName) ?:
                return ResponseEntity.notFound().build()

        if (!cacheManager.cacheNames.contains(cacheName)) {
            return ResponseEntity.notFound().build()
        }

        val cache = cacheManager.getCache(cacheName)

        val keyExtractor = CacheKeysExtractorFactory.getKeyExtractor(cache)
        return ResponseEntity.ok(keyExtractor.extractKeys(cache))
    }
}