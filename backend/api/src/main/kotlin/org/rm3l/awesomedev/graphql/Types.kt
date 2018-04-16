package org.rm3l.awesomedev.graphql

data class ArticleFilter(
        val from: String? = null,
        val to: String? = null,
        val search: String? = null,
        val tags: List<String>? = null,
        val title: String? = null,
        val url: String? = null
)

data class ArticleInput(
       val date: String,
       val description: String,
       val url: String,
       val tags: List<String>? = null
)