package org.rm3l.tools.reverseproxy.services

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import org.rm3l.tools.reverseproxy.controllers.X_FORWARDED_FOR
import org.rm3l.tools.reverseproxy.controllers.X_FORWARDED_HOST
import org.rm3l.tools.reverseproxy.controllers.X_FORWARDED_PROTO
import org.rm3l.tools.reverseproxy.resources.ProxyData
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RequestMethod
import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletRequest

@Service
class ReverseProxyService(restTemplateBuilder: RestTemplateBuilder) {

    private val logger = LoggerFactory.getLogger(ReverseProxyService::class.java)

    private val restTemplate = restTemplateBuilder.build()

    @Value("\${cache.size:100000}")
    private val cacheSize: Long = 100000

    private var cache: Cache<String, ResponseEntity<String>>? = null

    @PostConstruct
    fun init() {
        cache = CacheBuilder.newBuilder()
        .maximumSize(cacheSize)
                .build()
    }

    fun exchangeWithRemoteServer(request: HttpServletRequest, proxyData: ProxyData): ResponseEntity<String> {
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

        val responseEntity: ResponseEntity<String>
        val responseEntityFromCache = cache?.getIfPresent(targetHost)
        if (proxyData.forceRequest == true || responseEntityFromCache == null) {
            responseEntity = this.restTemplate.exchange(targetHost,
                    HttpMethod.resolve((proxyData.requestMethod?:RequestMethod.GET).name),
                    requestEntity,
                    String::class.java,
                    proxyData.requestParams ?: emptyMap<String, String>())
            cache?.put(targetHost, responseEntity)
        } else {
            responseEntity = responseEntityFromCache
        }

        if (logger.isDebugEnabled) {
            logger.debug("<<< [${responseEntity.statusCodeValue}] $requestStr : ${responseEntity.body}")
        }

        return responseEntity
    }
}