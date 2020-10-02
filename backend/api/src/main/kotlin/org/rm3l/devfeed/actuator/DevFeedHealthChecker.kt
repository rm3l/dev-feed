package org.rm3l.devfeed.actuator

import org.rm3l.devfeed.persistence.DevFeedDao
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Service

@Service
class DevFeedHealthChecker(private val dao: DevFeedDao): HealthIndicator {

  override fun health(): Health =
    if (dao.getRecentArticles(limit = 1).isNotEmpty()) {
      Health.up().build()
    } else {
      Health.down().build()
    }
}
