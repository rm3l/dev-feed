package org.rm3l.devfeed.common.utils

import java.text.SimpleDateFormat
import java.util.Date

fun Date?.asSupportedTimestamp() =
        SimpleDateFormat("yyyy-MM-dd").format(this).asSupportedTimestamp()
