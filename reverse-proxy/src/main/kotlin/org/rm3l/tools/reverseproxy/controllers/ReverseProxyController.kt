package org.rm3l.tools.reverseproxy.controllers

import com.fasterxml.jackson.annotation.JsonIgnore
import org.rm3l.tools.reverseproxy.resources.ProxyData
import org.slf4j.LoggerFactory
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.*
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.servlet.http.HttpServletRequest

@RestController
class ReverseProxyController(restTemplateBuilder: RestTemplateBuilder) {

    private val logger = LoggerFactory.getLogger(ReverseProxyController::class.java)

    private val restTemplate = restTemplateBuilder.build()

    @PostMapping(value = "/proxy")
    @ResponseBody
    fun proxyRequest(@RequestBody(required = true) proxyData: ProxyData,
                     request: HttpServletRequest): ResponseEntity<String> {
        val targetHost = proxyData.targetHost?:"${request.scheme}://${request.serverName}:${request.serverPort}"
        val requestHeaders: MutableMap<String, List<String>> = mutableMapOf(
                //TODO
                "X-Forwarded-For" to emptyList(),
                "X-Forwarded-Host" to emptyList()
        )
        proxyData.requestHeaders?.let { requestHeaders.putAll(it) }
        val httpHeaders = HttpHeaders()
        httpHeaders.putAll(requestHeaders)
        val requestEntity = HttpEntity(proxyData.requestBody, httpHeaders)


        val requestStr = "${proxyData.requestMethod?:RequestMethod.GET}?${proxyData.requestParams?:""} " +
                "$targetHost -H '$requestHeaders' -d '${proxyData.requestBody}'"
        if (logger.isDebugEnabled) {
            logger.debug(">>> $requestStr")
        }

        val responseEntity = this.restTemplate.exchange(targetHost,
                HttpMethod.resolve((proxyData.requestMethod?:RequestMethod.GET).name),
                requestEntity,
                String::class.java,
                proxyData.requestParams ?: emptyMap<String, String>())

        if (logger.isDebugEnabled) {
            logger.debug("<<< [${responseEntity.statusCodeValue}] $requestStr : ${responseEntity.body}")
        }

        return responseEntity
    }

}

@RestControllerAdvice
class ReverseProxyControllerAdvice {

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleAnyException(e: Throwable) = ReverseProxyControllerException(e)

}

@Suppress("unused")
data class ReverseProxyControllerException(@JsonIgnore private val cause: Throwable) {
    private val _id = UUID.randomUUID().toString()
    private val timestamp = System.currentTimeMillis()
    private val errorMessage = "${cause::class.java.simpleName} : ${cause.message}"
}