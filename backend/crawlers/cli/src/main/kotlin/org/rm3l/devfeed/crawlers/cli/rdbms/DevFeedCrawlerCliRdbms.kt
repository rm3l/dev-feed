package org.rm3l.devfeed.crawlers.cli.rdbms

import org.rm3l.devfeed.crawlers.common.DEFAULT_THREAD_POOL_SIZE
import org.rm3l.devfeed.crawlers.common.DevFeedCrawler
import org.rm3l.devfeed.persistence.impl.rdbms.DevFeedRdbmsDao
import org.rm3l.devfeed.persistence.utils.handleAndPersistIfNeeded
import org.rm3l.devfeed.screenshot.impl.GooglePageSpeedOnlineScreenshotExtractor
import picocli.CommandLine
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@CommandLine.Command(name = "rdbms",
  description = ["Fetch and persist articles in a relational data store"])
class DevFeedCrawlerCliRdbms: Runnable {

  @CommandLine.Option(names = ["-c", "--crawler"],
    description = ["the crawler canonical class name"])
  private lateinit var crawlerType: Class<DevFeedCrawler>

  @CommandLine.Option(names = ["-t", "--thread-pool-size"],
    description = [
      "the thread pool size for fetching articles (default: \${DEFAULT-VALUE})"])
  private var threadPoolSize: String = "$DEFAULT_THREAD_POOL_SIZE"

  @CommandLine.Option(names = ["-j", "--datasource-jdbc-url"],
    description = ["the JDBC URL"])
  private lateinit var datasourceUrl: String

  @CommandLine.Option(names = ["-d", "--datasource-jdbc-driver"],
    description = ["the JDBC URL"])
  private lateinit var datasourceDriver: String

  @CommandLine.Option(names = ["-u", "--datasource-user"],
    description = ["the datasource username"])
  private lateinit var datasourceUser: String

  @CommandLine.Option(names = ["-p", "--datasource-password"],
    description = ["the datasource password"])
  private lateinit var datasourcePassword: String

  @CommandLine.Option(names = ["-s", "--screenshot-parser-pagespeed-online-key"],
    description = ["the PageSpeed Online API Key for screenshot extraction"])
  private var screenshotPageSpeedOnlineKey: String? = null

  override fun run() {
    val executorService = Executors.newFixedThreadPool(threadPoolSize.toInt())
    val crawler = try {
      crawlerType.getConstructor(ExecutorService::class.java).newInstance(executorService)
    } catch (nsme: NoSuchMethodException) {
      //Default to default constructor, if any
      crawlerType.getConstructor().newInstance()
    }

    val dao = DevFeedRdbmsDao(datasourceUrl = datasourceUrl,
      datasourceDriver = datasourceDriver,
      datasourceUser = datasourceUser,
      datasourcePassword = datasourcePassword)

    val articles = crawler?.call()
    println("Fetched ${articles?.size} articles using $crawlerType")

    articles.handleAndPersistIfNeeded(dao = dao,
      executorService = executorService,
      articleScreenshotExtractor =
      if (screenshotPageSpeedOnlineKey.isNullOrBlank())
        null
      else GooglePageSpeedOnlineScreenshotExtractor(
        dao = dao,
        pageSpeedOnlineApiKey = screenshotPageSpeedOnlineKey!!),
      synchronous = true)
  }
}
