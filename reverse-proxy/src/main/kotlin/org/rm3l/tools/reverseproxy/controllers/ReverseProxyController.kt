package org.rm3l.tools.reverseproxy.controllers

import com.fasterxml.jackson.annotation.JsonIgnore
import org.rm3l.tools.reverseproxy.resources.ProxyData
import org.rm3l.tools.reverseproxy.services.ReverseProxyService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.*
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.*
import java.util.*
import javax.servlet.http.HttpServletRequest

const val USER_AGENT = "User-Agent"
const val X_FORWARDED_FOR = "X-Forwarded-For"
const val X_FORWARDED_HOST = "X-Forwarded-Host"
const val X_FORWARDED_PROTO = "X-Forwarded-Proto"

@RestController
class ReverseProxyController {

    private val logger = LoggerFactory.getLogger(ReverseProxyController::class.java)

    @Autowired
    lateinit var reverseProxyService: ReverseProxyService

    @PostMapping(value = "/proxy")
    @ResponseBody
    fun proxyRequest(@RequestBody(required = true) proxyData: ProxyData,
                     request: HttpServletRequest): ResponseEntity<*> {

        val requester = request.remoteAddr?:request.getHeader(X_FORWARDED_FOR)

        if (logger.isDebugEnabled) {
            logger.debug("POST /proxy - " +
                    "origin=${requester ?: "???"}, " +
                    "user-agent=[${request.getHeader(USER_AGENT)}], " +
                    "content-type=[${request.contentType}] - " +
                    "payload=$proxyData")
        }

        return reverseProxyService.exchangeWithRemoteServer(request, proxyData)
    }

}

@RestControllerAdvice
class ReverseProxyControllerAdvice {

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleHttpClientException(e: HttpClientErrorException) = wrapException(e)

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleHttpClientException(e: ResourceAccessException) = wrapException(e)

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
                    isClientError = e is HttpClientErrorException || e is ResourceAccessException)
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