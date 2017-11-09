package org.rm3l.tools.reverseproxy.services.ip_geo

import org.rm3l.tools.reverseproxy.resources.ip_geo.NetWhoisInfo

interface IPGeolocationService<T> where T:NetWhoisInfo {

    fun resolve(vararg data: String): Map<String, T>

    fun getTargetUrl(host: String): String

    fun getResponseType(): Class<T>
}