package org.rm3l.devfeed.crawlers.impl.fastvoted

import com.slack.api.Slack
import com.slack.api.model.event.MessageEvent
import com.slack.api.rtm.RTMClient
import com.slack.api.rtm.RTMEventHandler
import com.slack.api.rtm.RTMEventsDispatcherFactory
import org.rm3l.devfeed.crawlers.DevFeedFetcherService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import javax.websocket.CloseReason


@Service
@ConditionalOnProperty(name = ["crawlers.fastvoted.enabled"], havingValue = "true", matchIfMissing = false)
class FastvotedCrawler {

    companion object {
        @JvmStatic
        private val logger = LoggerFactory.getLogger(FastvotedCrawler::class.java)
    }

    @Value("\${crawlers.fastvoted.slack.botToken}")
    private lateinit var slackApiToken: String

    @Value("\${crawlers.fastvoted.slack.channelId}")
    private lateinit var slackChannelId: String

    @Autowired
    private lateinit var devFeedFetcherService: DevFeedFetcherService

    private lateinit var slack: Slack
    private lateinit var rtmClient: RTMClient

    @PostConstruct
    fun init() {
        slack = Slack.getInstance()

        // Dispatches incoming message events from RTM API
        val dispatcher = RTMEventsDispatcherFactory.getInstance()
        val messageEventHandler = object: RTMEventHandler<MessageEvent>() {
            override fun handle(event: MessageEvent?) {
                if (event == null) {
                    logger.debug("Received NULL event => aborting.")
                    return
                }
                if (slackChannelId != event.channel) {
                    logger.debug("Received message in a different channel than the one expected " +
                            "=> aborting.")
                    return
                }

                TODO("Not yet implemented")
            }
        }
        dispatcher.register(messageEventHandler)

        rtmClient = slack.rtm(slackApiToken)
        rtmClient.addCloseHandler {reason ->
            logger.debug("Close Handler. Reason is $reason")
            if (reason.closeCode == CloseReason.CloseCodes.CLOSED_ABNORMALLY) {
                //Reconnect
                logger.debug("Reconnecting...")
                rtmClient.reconnect()
            }
        }
        rtmClient.addErrorHandler {
            logger.debug("Error Handler: ", it)

        }

        rtmClient.addMessageHandler(dispatcher.toMessageHandler())
    }

    @PreDestroy
    fun destroy() {
        try {
            rtmClient.disconnect()
        } finally {
            slack.close()
        }
    }

}

