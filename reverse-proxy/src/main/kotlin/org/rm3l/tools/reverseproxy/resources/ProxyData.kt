package org.rm3l.tools.reverseproxy.resources

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.web.bind.annotation.RequestMethod

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProxyData(val forceRequest: Boolean? = false,
                     val targetHost: String? = null,
                     val requestMethod: RequestMethod? = null,
                     val requestHeaders: Map<String, List<String>>? = null,
                     val requestParams: Map<String, String>? = null,
                     val requestBody: String? = null)