package org.rm3l.tools.reverseproxy.utils

/**
 * Return the root cause or the current throwable if no cause
 */
fun Throwable.getRootCause(): Throwable {
    var currentThrowableCause = this.cause
    while (currentThrowableCause != null) {
        currentThrowableCause = currentThrowableCause.cause
    }
    return currentThrowableCause?:this
}
