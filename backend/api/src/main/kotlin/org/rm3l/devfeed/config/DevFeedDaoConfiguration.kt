package org.rm3l.devfeed.config

import org.rm3l.devfeed.persistence.DevFeedDao
import org.rm3l.devfeed.persistence.rdbms.DevFeedRdbmsDao
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

@Configuration
class DevFeedDaoConfiguration {

  @Bean
  fun devFeedDao(
    @Value("\${datasource.url}") datasourceUrl: String,
    @Value("\${datasource.driver}") datasourceDriver: String,
    @Value("\${datasource.user}") datasourceUser: String,
    @Value("\${datasource.password}") datasourcePassword: String,
    @Value("\${datasource.poolSize}") datasourcePoolSize: String,
    @Autowired jackson2ObjectMapperBuilder: Jackson2ObjectMapperBuilder
  ): DevFeedDao = DevFeedRdbmsDao(
    datasourceUrl = datasourceUrl,
    datasourceDriver = datasourceDriver,
    datasourceUser = datasourceUser,
    datasourcePassword = datasourcePassword,
    datasourcePoolSize = datasourcePoolSize.toInt(),
    objectMapper = jackson2ObjectMapperBuilder.build())
}
