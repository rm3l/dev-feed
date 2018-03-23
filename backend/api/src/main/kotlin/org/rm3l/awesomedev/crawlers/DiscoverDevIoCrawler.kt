package org.rm3l.awesomedev.crawlers

import org.jetbrains.exposed.sql.Op
import org.jsoup.Jsoup
import org.rm3l.awesomedev.dal.AwesomeDevDao
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.Executors

const val BACKEND_BASE_URL = "http://www.discoverdev.io"
const val BACKEND_ARCHIVE_URL = "$BACKEND_BASE_URL/archive"
const val USER_AGENT = "org.rm3l.discoverdev_io"

@Component
class DiscoverDevIoCrawler(val dao: AwesomeDevDao) {

    private val executorService = Executors.newSingleThreadExecutor()

    fun triggerRemoteWebsiteCrawling() {
        Jsoup.connect(BACKEND_ARCHIVE_URL)
                .userAgent(USER_AGENT)
                .get()
                .run {
                    select("main.archive-page ul.archive-list li a").map { element ->
                        element.attr("href")
                                .replaceFirst("/archive/", "", ignoreCase = true)
                    }.forEach { date ->
                        executorService.submit(DiscoverDevIoArchiveCrawler(dao, date))
                    }
                }
    }
}

class DiscoverDevIoArchiveCrawler(private val dao: AwesomeDevDao, private val date: String): Runnable {

    companion object {
        @JvmStatic
        private val logger = LoggerFactory.getLogger(DiscoverDevIoCrawler::class.java)
    }

    override fun run() {
        if (logger.isDebugEnabled) {
            logger.debug(">>> Crawling archive data for $date")
        }
        Jsoup.connect("$BACKEND_ARCHIVE_URL/$date")
                .userAgent(USER_AGENT)
                .get()
                .run {
                    select("main.archive-page ul.archive-list li.post-item")
                            .forEachIndexed { index, element ->
                                val titleAndLink = element.select("h1.title a")
                                val title = titleAndLink.text()
                                val url = titleAndLink.attr("href")

                                val tags = element.select("p.tags a.tlink")
                                        .map { tagElement -> tagElement.text() }
                                        .toSet()
                                if (logger.isDebugEnabled) {
                                    logger.debug("$index. $date - $title ($url) / $tags")
                                }

                                //Check if (title, url) pair already exist in the DB
                                val existArticlesByTitleAndUrl = dao.existArticlesByTitleAndUrl(title, url)
                                if (logger.isDebugEnabled) {
                                    logger.debug("$existArticlesByTitleAndUrl = existArticlesByTitleAndUrl($title, $url)")
                                }
                                if (!existArticlesByTitleAndUrl) {
                                    dao.insertArticleAndTags(date, title, null, url, tags)
                                }
                            }
                }
    }

}