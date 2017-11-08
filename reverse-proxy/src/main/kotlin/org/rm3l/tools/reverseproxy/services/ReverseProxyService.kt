package org.rm3l.tools.reverseproxy.services

import org.rm3l.tools.reverseproxy.controllers.X_FORWARDED_FOR
import org.rm3l.tools.reverseproxy.controllers.X_FORWARDED_HOST
import org.rm3l.tools.reverseproxy.controllers.X_FORWARDED_PROTO
import org.rm3l.tools.reverseproxy.resources.ProxyData
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RequestMethod
import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletRequest

@Service
class ReverseProxyService(@Qualifier("proxyResponseCacheManager") private val cacheManager: CacheManager,
                          restTemplateBuilder: RestTemplateBuilder) {

    private val logger = LoggerFactory.getLogger(ReverseProxyService::class.java)

    private val restTemplate = restTemplateBuilder.build()

    private var proxyResponseCache : Cache? = null

    @PostConstruct
    fun init() {
        proxyResponseCache = cacheManager.getCache("proxyResponseCache")
    }

    fun exchangeWithRemoteServer(request: HttpServletRequest, proxyData: ProxyData): ResponseEntity<*> {
        val targetHost = proxyData.targetHost?:"${request.scheme}://${request.serverName}:${request.serverPort}"
        val requestHeaders: MutableMap<String, List<String>> = mutableMapOf(
                X_FORWARDED_FOR to setOf(request.remoteAddr?:request.getHeader(X_FORWARDED_FOR)?:"").toList(),
                X_FORWARDED_HOST to listOf(request.remoteHost?:""),
                X_FORWARDED_PROTO to listOf(request.protocol?:"")
        )
        proxyData.requestHeaders?.let { requestHeaders.putAll(it) }
        val httpHeaders = HttpHeaders()
        httpHeaders.putAll(requestHeaders)
        val requestEntity = HttpEntity(proxyData.requestBody, httpHeaders)

        val requestStr = "${proxyData.requestMethod?: RequestMethod.GET}?${proxyData.requestParams?:""} " +
                "$targetHost -H '$requestHeaders' -d '${proxyData.requestBody}'"
        if (logger.isDebugEnabled) {
            logger.debug(">>> $requestStr")
        }

        val responseEntity: ResponseEntity<*>
        val responseEntityFromCache = proxyResponseCache?.get(targetHost)
        if (proxyData.forceRequest == true || responseEntityFromCache == null) {
            responseEntity = this.restTemplate.exchange(targetHost,
                    HttpMethod.resolve((proxyData.requestMethod?:RequestMethod.GET).name),
                    requestEntity,
                    String::class.java,
                    proxyData.requestParams ?: emptyMap<String, String>())
            proxyResponseCache?.put(targetHost, responseEntity)
        } else {
            responseEntity = responseEntityFromCache.get() as ResponseEntity<*>
        }

        if (logger.isDebugEnabled) {
            logger.debug("<<< [${responseEntity.statusCodeValue}] $requestStr : ${responseEntity.body}")
        }

        return responseEntity
    }
}