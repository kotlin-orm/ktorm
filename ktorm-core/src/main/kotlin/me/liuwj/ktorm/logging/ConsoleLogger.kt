package me.liuwj.ktorm.logging

/**
 * Simple [Logger] implementation printing logs to the console. While messages at WARN or ERROR levels are printed to
 * [System.err], others are printed to [System.out].
 *
 * @property threshold a threshold controlling which log levels are enabled.
 */
class ConsoleLogger(val threshold: LogLevel) : Logger {

    override fun isTraceEnabled(): Boolean {
        return LogLevel.TRACE >= threshold
    }

    override fun trace(msg: String, e: Throwable?) {
        log(LogLevel.TRACE, msg, e)
    }

    override fun isDebugEnabled(): Boolean {
        return LogLevel.DEBUG >= threshold
    }

    override fun debug(msg: String, e: Throwable?) {
        log(LogLevel.DEBUG, msg, e)
    }

    override fun isInfoEnabled(): Boolean {
        return LogLevel.INFO >= threshold
    }

    override fun info(msg: String, e: Throwable?) {
        log(LogLevel.INFO, msg, e)
    }

    override fun isWarnEnabled(): Boolean {
        return LogLevel.WARN >= threshold
    }

    override fun warn(msg: String, e: Throwable?) {
        log(LogLevel.WARN, msg, e)
    }

    override fun isErrorEnabled(): Boolean {
        return LogLevel.ERROR >= threshold
    }

    override fun error(msg: String, e: Throwable?) {
        log(LogLevel.ERROR, msg, e)
    }

    private fun log(level: LogLevel, msg: String, e: Throwable?) {
        if (level >= threshold) {
            val out = if (level >= LogLevel.WARN) System.err else System.out
            out.println("[$level] $msg")
            e?.printStackTrace(out)
        }
    }
}
