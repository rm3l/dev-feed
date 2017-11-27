package org.rm3l.iana.service_names_port_numbers.parsers

import org.rm3l.iana.service_names_port_numbers.domain.Record

interface ServiceNamePortNumberMappingParser {

    enum class Format {
        CSV,
        XML,
        //HTML,
        //TEXT
    }

    fun parse(content: String): List<Record>

}
