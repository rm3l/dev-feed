package org.rm3l.iana.service_names_port_numbers.resolvers

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import com.github.benmanes.caffeine.cache.LoadingCache
import org.rm3l.iana.service_names_port_numbers.domain.Record
import org.rm3l.iana.service_names_port_numbers.domain.RecordFilter
import org.rm3l.iana.service_names_port_numbers.parsers.ServiceNamePortNumberMappingParser

class Query(private val cache: LoadingCache<ServiceNamePortNumberMappingParser.Format, List<Record>>) : GraphQLQueryResolver {

    fun records(filter: RecordFilter?): List<Record> {
        val fullListOfRecords: List<Record> = cache.get(ServiceNamePortNumberMappingParser.Format.XML)
                ?: throw IllegalStateException("Failed to fetch XML content")
        return if (filter == null) {
            fullListOfRecords
        } else {
            fullListOfRecords
                    .filter { filter.ports?.contains(it.portNumber) ?: true }
                    .filter { filter.protocols?.contains(it.transportProtocol) ?: true }
                    .filter { filter.services?.contains(it.serviceName) ?: true }
        }
    }
}