package org.rm3l.iana.service_names_port_numbers.configuration

import com.coxautodev.graphql.tools.SchemaParser
import com.github.benmanes.caffeine.cache.LoadingCache
import graphql.schema.GraphQLSchema
import org.rm3l.iana.service_names_port_numbers.domain.Record
import org.rm3l.iana.service_names_port_numbers.parsers.ServiceNamePortNumberMappingParser
import org.rm3l.iana.service_names_port_numbers.resolvers.Query
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import java.io.File

@Configuration
class GraphQLConfiguration(@Qualifier(value = "ianaServiceNamePortNumbersRemoteCache")
                           val cache: LoadingCache<ServiceNamePortNumberMappingParser.Format, List<Record>>) {

    @Bean
    fun graphQLSchema(): GraphQLSchema {
        val allSchemas = PathMatchingResourcePatternResolver()
                .getResources("/schema/**/*.graphql")
                .map { "schema${File.separator}${it.filename}" }
                .toList()
        return SchemaParser.newParser()
                .files(*allSchemas.toTypedArray())
                .resolvers(Query(cache))
                .build()
                .makeExecutableSchema()
    }

}