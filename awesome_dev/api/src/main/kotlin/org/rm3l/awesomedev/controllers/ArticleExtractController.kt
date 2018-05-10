package org.rm3l.awesomedev.controllers

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import de.jetwick.snacktory.HtmlFetcher



@RestController
class ArticleExtractController {

    @GetMapping("/labs/extractArticle")
    @ResponseBody
    fun extractArticle(@RequestParam(name = "link", required = true) link: String): String {
        val fetcher = HtmlFetcher()
        val res = fetcher.fetchAndExtract(link, 30_000, true)
        val text = res.text
        val title = res.title
        val imageUrl = res.imageUrl
        return text
    }
}
