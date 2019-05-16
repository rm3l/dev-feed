package org.rm3l.awesomedev.utils

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun String?.asSupportedTimestamp() =
        this?.let {
            1000L * LocalDateTime
                    .parse("${it}T12:00:00", DateTimeFormatter.ISO_DATE_TIME)
                    .toEpochSecond(ZoneOffset.UTC)
        }