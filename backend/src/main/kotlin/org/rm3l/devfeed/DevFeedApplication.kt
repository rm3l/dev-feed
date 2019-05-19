//The MIT License (MIT)
//
//Copyright (c) 2019 Armel Soro
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to deal
//in the Software without restriction, including without limitation the rights
//to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in all
//copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//SOFTWARE.
package org.rm3l.devfeed

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.PropertySource
import org.springframework.context.annotation.PropertySources
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Entry-point
 */
@SpringBootApplication
@PropertySources(value = [
    //The order matters here. If a same property key is found in many files, the last one wins.
    PropertySource(value = ["classpath:application.properties"]),
    PropertySource(value = ["file:/etc/dev-feed/backend.properties"], ignoreResourceNotFound = true)]
)
@EnableScheduling
class DevFeedApplication

fun main(args: Array<String>) {
    SpringApplication.run(DevFeedApplication::class.java, *args)
}

