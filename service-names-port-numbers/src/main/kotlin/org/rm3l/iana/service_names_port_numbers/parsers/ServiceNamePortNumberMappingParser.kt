package org.rm3l.iana.service_names_port_numbers.parsers

import org.rm3l.iana.service_names_port_numbers.domain.Record

interface ServiceNamePortNumberMappingParser {

    enum class Format {
        XML,
        
        //We might handle other formats here
        //CSV,
        //HTML,
        //TEXT
    }

    fun parse(content: String): List<Record>

}
