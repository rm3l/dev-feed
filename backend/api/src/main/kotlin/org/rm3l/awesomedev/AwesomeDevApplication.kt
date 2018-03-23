package org.rm3l.awesomedev

import org.rm3l.awesomedev.crawlers.DiscoverDevIoCrawler
import org.rm3l.awesomedev.dal.AwesomeDevDao
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.PropertySource
import org.springframework.context.annotation.PropertySources
import org.springframework.stereotype.Component

@SpringBootApplication
@PropertySources(value = [
    //The order matters here. If a same property key is found in many files, the last one wins.
    PropertySource(value = ["classpath:application.properties"]),
    PropertySource(value = ["file:/etc/rm3l/awesome-dev.properties"], ignoreResourceNotFound = true)]
)
class AwesomeDevApplication

fun main(args: Array<String>) {
    SpringApplication.run(AwesomeDevApplication::class.java, *args)
}

@Component
class AwesomeDevApplicationRunner(val crawler: DiscoverDevIoCrawler, val dao: AwesomeDevDao) : CommandLineRunner {

    override fun run(vararg args: String?) {
        dao.createDatabase()
        crawler.triggerRemoteWebsiteCrawling()
    }
}
