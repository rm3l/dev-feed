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
package org.rm3l.devfeed.common.contract

import java.net.URL

data class Article(val id: String? = null,
                   val timestamp: Long = System.currentTimeMillis(),
                   val title: String,
                   val description: String? = null,
                   val url: String,
                   val domain: String = URL(url).host,
                   var tags: Collection<String?>? = setOf(),
                   var screenshot: Screenshot? = null,
                   var parsed: ArticleParsed? = null,
                   var source: String? = null)

data class Screenshot(val data: String? = null,
                      val height: Int? = null,
                      val width: Int? = null,
                      val mimeType: String? = null)

data class ArticleParsed(val url: String,
                         val title: String? = null,
                         val author: String? = null,
                         val published: String? = null, //TODO Use DateTime
                         val image: String? = null,
                         val videos: Collection<String?>? = null,
                         val keywords: Collection<String?>? = null,
                         val description: String? = null,
                         val body: String)
