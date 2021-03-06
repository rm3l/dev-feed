/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Armel Soro
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.rm3l.devfeed.config

import org.rm3l.devfeed.persistence.DevFeedDao
import org.rm3l.devfeed.persistence.impl.mongodb.DevFeedMongoDbDao
import org.rm3l.devfeed.persistence.impl.rdbms.DevFeedRdbmsDao
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

@Configuration
class DevFeedDaoConfiguration {

  @Bean(destroyMethod = "close")
  @ConditionalOnProperty(name = ["datasource.type"], havingValue = "rdbms", matchIfMissing = true)
  fun rdbmsDevFeedDao(
      @Value("\${datasource.url}") datasourceUrl: String,
      @Value("\${datasource.driver}") datasourceDriver: String,
      @Value("\${datasource.user}") datasourceUser: String,
      @Value("\${datasource.password}") datasourcePassword: String,
      @Value("\${datasource.poolSize}") datasourcePoolSize: String,
      @Autowired jackson2ObjectMapperBuilder: Jackson2ObjectMapperBuilder
  ): DevFeedDao =
      DevFeedRdbmsDao(
          datasourceUrl = datasourceUrl,
          datasourceDriver = datasourceDriver,
          datasourceUser = datasourceUser,
          datasourcePassword = datasourcePassword,
          datasourcePoolSize = datasourcePoolSize.toInt(),
          objectMapper = jackson2ObjectMapperBuilder.build())

  @Bean(destroyMethod = "close")
  @ConditionalOnProperty(
      name = ["datasource.type"], havingValue = "mongodb", matchIfMissing = false)
  fun mongodbDevFeedDao(
      @Value("\${datasource.mongodb.connectionString}") datasourceMongoDbConnectionString: String
  ): DevFeedDao = DevFeedMongoDbDao(datasourceMongoDbConnectionString)
}
