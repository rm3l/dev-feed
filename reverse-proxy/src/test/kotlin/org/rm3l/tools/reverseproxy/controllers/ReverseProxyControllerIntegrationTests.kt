package org.rm3l.tools.reverseproxy.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.rm3l.tools.reverseproxy.ReverseProxyApplication
import org.rm3l.tools.reverseproxy.resources.ProxyData
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.context.ActiveProfiles

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.*


@RunWith(SpringRunner::class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = arrayOf(ReverseProxyApplication::class))
@ActiveProfiles(profiles = arrayOf("test"))
@AutoConfigureMockMvc
class ReverseProxyControllerIntegrationTests {

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var mvc: MockMvc

    @Value("\${security.user.password}")
    lateinit var securityUserPassword: String

    @Test
    fun testProxyRequestToExternalServer_AuthenticationRequired() {
        this.mvc.perform (
                post("/proxy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized)
    }

    @Test
    fun testProxyRequestToExternalServer_DefaultMethod() {
        val proxyData = ProxyData(forceRequest = false, targetHost = "http://www.google.com")
        val contentAsString = this.mvc.perform(
                post("/proxy")
                        .with(httpBasic("user", securityUserPassword))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(proxyData)))
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString
        Assert.assertFalse(contentAsString.isBlank())
    }

    @Test
    fun testProxyRequestToExternalServer_4xxError() {
        val proxyData = ProxyData(forceRequest = false, targetHost = "http://${UUID.randomUUID()}.rm3l.org")
        this.mvc.perform (
                post("/proxy")
                        .with(httpBasic("user", securityUserPassword))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(proxyData)))
                .andExpect(status().isBadRequest)
    }

}