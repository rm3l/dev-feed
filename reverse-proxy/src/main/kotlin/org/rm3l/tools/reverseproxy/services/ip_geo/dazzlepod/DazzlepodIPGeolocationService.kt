package org.rm3l.tools.reverseproxy.services.ip_geo.dazzlepod

import org.rm3l.tools.reverseproxy.services.ip_geo.IPGeolocationService
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.stereotype.Service

@Service("dazzlepod.com")
class DazzlepodIPGeolocationService(restTemplateBuilder: RestTemplateBuilder):
        IPGeolocationService<DazzlepodNetWhoisInfo> {

    companion object {
        val API_ENDPOINT = "https://dazzlepod.com/ip"
    }

    private val restTemplate = restTemplateBuilder.build()

    override fun getResponseType() = DazzlepodNetWhoisInfo::class.java

    override fun getTargetUrl(host: String) = "$API_ENDPOINT/${host.trim()}.json"

    override fun resolve(vararg data: String): Map<String, DazzlepodNetWhoisInfo> =
            data.map { it.trim() }
                    .map {
                        it to restTemplate
                                .getForObject("$API_ENDPOINT/${it.trim()}.json", DazzlepodNetWhoisInfo::class.java)
                    }
                    .toMap()
}
