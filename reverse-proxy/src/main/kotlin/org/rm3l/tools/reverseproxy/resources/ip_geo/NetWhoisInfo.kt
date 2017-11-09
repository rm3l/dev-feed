package org.rm3l.tools.reverseproxy.resources.ip_geo

interface NetWhoisInfo

data class NetWhoisInfoApiResponse(val host: String, val info: NetWhoisInfo)