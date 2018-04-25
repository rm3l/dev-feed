package org.rm3l.awesomedev.graphql

data class ArticleFilter(
        val from: Long? = null,
        val to: Long? = null,
        val search: String? = null,
        val tags: List<String>? = null,
        val titles: List<String>? = null,
        val urls: List<String>? = null
)

data class ArticleInput(
       val timestamp: Long,
       val description: String,
       val url: String,
       val tags: List<String>? = null
)