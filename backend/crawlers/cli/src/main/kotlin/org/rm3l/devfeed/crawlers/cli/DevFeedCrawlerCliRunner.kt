//@file:JvmName("DevFeedCrawlerCliRunner")
package org.rm3l.devfeed.crawlers.cli

import org.rm3l.devfeed.crawlers.cli.rdbms.DevFeedCrawlerCliRdbms
import picocli.CommandLine
import picocli.CommandLine.Model.CommandSpec
import picocli.CommandLine.ParameterException

@CommandLine.Command(description = ["Fetch articles"],
  mixinStandardHelpOptions = true,
  version = ["0.10.5"],
  subcommands = [DevFeedCrawlerCliRdbms::class])
class DevFeedCrawlerCliRunner : Runnable {

  @CommandLine.Spec
  lateinit var spec: CommandSpec

  companion object {

    private val cmd: CommandLine by lazy {
      CommandLine(DevFeedCrawlerCliRunner())
    }

    @JvmStatic
    fun main(args: Array<String>) {
      if (args.isEmpty()) {
        showHelp()
      } else {
        cmd.execute(*args)
      }
    }

    @JvmStatic
    fun showHelp() {
      cmd.usage(System.out)
    }
  }

  override fun run() {
    throw ParameterException(spec.commandLine(), "Please specify a subcommand")
  }
}

