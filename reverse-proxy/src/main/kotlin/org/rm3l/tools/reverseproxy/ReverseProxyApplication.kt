package org.rm3l.tools.reverseproxy

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class ReverseProxyApplication

fun main(args: Array<String>) {
    SpringApplication.run(ReverseProxyApplication::class.java, *args)
}
