package org.rm3l.tools.reverseproxy.controllers

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import org.rm3l.tools.reverseproxy.resources.ProxyData
import org.rm3l.tools.reverseproxy.resources.ip_geo.NetWhoisInfoApiResponse
import org.rm3l.tools.reverseproxy.services.ReverseProxyService
import org.rm3l.tools.reverseproxy.services.ip_geo.IPGeolocationService
import org.rm3l.tools.reverseproxy.utils.getRootCause
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
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
class ReverseProxyController(val objectMapper: ObjectMapper): ApplicationContextAware {

    private val logger = LoggerFactory.getLogger(ReverseProxyController::class.java)

    private lateinit var applicationContext: ApplicationContext

    @Value("\${service.ipGeolocation}")
    private var ipGeolocationServiceName: String? = null

    override fun setApplicationContext(applicationContext: ApplicationContext?) {
        this.applicationContext = applicationContext!!
    }

    @Autowired
    lateinit var reverseProxyService: ReverseProxyService

    //General-purpose service
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

    //Specific: IP Geo Lookup service
    @PostMapping(value = "/proxy/networkGeoLocation")
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    fun geoLookupIP(@RequestBody(required = true) ipsOrHosts: Set<String>,
                    request: HttpServletRequest): Collection<NetWhoisInfoApiResponse> {
        val ipGeolocationService = applicationContext
                .getBean(this.ipGeolocationServiceName, IPGeolocationService::class.java)
        return ipsOrHosts
                .map { it to ProxyData(targetHost = ipGeolocationService.getTargetUrl(it)) }
                .map { it.first to reverseProxyService.exchangeWithRemoteServer(request, it.second).body.toString()}
                .map { it.first to objectMapper.readValue(it.second, ipGeolocationService.getResponseType()) }
                .map { NetWhoisInfoApiResponse(it.first, if (it.second.isNone()) null else it.second) }
                .toList()
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

    private val rootCause = cause.getRootCause()

    val errorId = UUID.randomUUID().toString()

    @Suppress("unused")
    val timestamp = System.currentTimeMillis()

    @Suppress("unused")
    val errorMessage =
            "${cause::class.java.simpleName} :" +
                    (if (cause != rootCause) rootCause::class.java.simpleName + " :" else "") +
                    " ${rootCause.message}"
}