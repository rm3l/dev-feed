package org.rm3l.devfeed.utils

import java.text.SimpleDateFormat
import java.util.*

fun Date?.asSupportedTimestamp() =
        SimpleDateFormat("yyyy-MM-dd").format(this).asSupportedTimestamp()
