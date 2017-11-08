package org.rm3l.tools.reverseproxy.controllers

import com.fasterxml.jackson.annotation.JsonIgnore
import org.rm3l.tools.reverseproxy.resources.ProxyData
import org.slf4j.LoggerFactory
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.*
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.UnknownHttpStatusCodeException
import java.util.*
import javax.servlet.http.HttpServletRequest

const val X_FORWARDED_FOR = "X-Forwarded-For"
const val X_FORWARDED_HOST = "X-Forwarded-Host"
const val X_FORWARDED_PROTO = "X-Forwarded-Proto"

@RestController
class ReverseProxyController(restTemplateBuilder: RestTemplateBuilder) {

    private val logger = LoggerFactory.getLogger(ReverseProxyController::class.java)

    private val restTemplate = restTemplateBuilder.build()

    @PostMapping(value = "/proxy")
    @ResponseBody
    fun proxyRequest(@RequestBody(required = true) proxyData: ProxyData,
                     request: HttpServletRequest): ResponseEntity<String> {

        val requester = request.remoteAddr?:request.getHeader(X_FORWARDED_FOR)

        logger.info("GET /proxy - " +
                "origin=${requester?:"???"}, " +
                "user-agent=[${request.getHeader("user-agent")}], " +
                "content-type=[${request.contentType}] - " +
                "payload=$proxyData")

        val targetHost = proxyData.targetHost?:"${request.scheme}://${request.serverName}:${request.serverPort}"
        val requestHeaders: MutableMap<String, List<String>> = mutableMapOf(
                X_FORWARDED_FOR to setOf<String>(requester?:"").toList(),
                X_FORWARDED_HOST to listOf(request.remoteHost?:""),
                X_FORWARDED_PROTO to listOf(request.protocol?:"")
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
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleHttpClientException(e: HttpClientErrorException) = wrapException(e)

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    fun handleHttpServerException(e: HttpServerErrorException) = wrapException(e)

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    fun handleUnknownHttpStatusCodeException(e: UnknownHttpStatusCodeException) = wrapException(e)

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    fun handleRestClientException(e: RestClientException) = wrapException(e)

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleAnyException(e: Throwable) = wrapException(e)

    companion object {

        private val logger = LoggerFactory.getLogger(ReverseProxyControllerAdvice::class.java)

        @JvmStatic
        fun wrapException(e: Throwable): ReverseProxyControllerErrorResponse {
            val errorResponse = ReverseProxyControllerErrorResponse(cause = e,
                    isClientError = e is HttpClientErrorException)
            if (errorResponse.isClientError) {
                logger.info(errorResponse.errorId, e)
            } else if (logger.isDebugEnabled) {
                logger.debug(errorResponse.errorId, e)
            }
            return errorResponse
        }
    }

}

data class ReverseProxyControllerErrorResponse(
        @JsonIgnore private val cause: Throwable,
        @JsonIgnore val isClientError: Boolean = false) {

    val errorId = UUID.randomUUID().toString()

    @Suppress("unused")
    private val timestamp = System.currentTimeMillis()

    @Suppress("unused")
    private val errorMessage = "${cause::class.java.simpleName} : ${cause.message}"
}