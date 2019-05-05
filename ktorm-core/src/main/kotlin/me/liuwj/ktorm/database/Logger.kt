package me.liuwj.ktorm.database

import java.util.logging.Level

/**
 * Created by vince on May 04, 2019.
 */
interface Logger {

    fun isTraceEnabled(): Boolean

    fun trace(msg: String, e: Throwable? = null)

    fun isDebugEnabled(): Boolean

    fun debug(msg: String, e: Throwable? = null)

    fun isInfoEnabled(): Boolean

    fun info(msg: String, e: Throwable? = null)

    fun isWarnEnabled(): Boolean

    fun warn(msg: String, e: Throwable? = null)

    fun isErrorEnabled(): Boolean

    fun error(msg: String, e: Throwable? = null)
}

enum class LogLevel {
    TRACE, DEBUG, INFO, WARN, ERROR
}

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

class JdkLoggerAdapter(val logger: java.util.logging.Logger) : Logger {

    override fun isTraceEnabled(): Boolean {
        return logger.isLoggable(Level.FINEST)
    }

    override fun trace(msg: String, e: Throwable?) {
        logger.log(Level.FINEST, msg, e)
    }

    override fun isDebugEnabled(): Boolean {
        return logger.isLoggable(Level.FINE)
    }

    override fun debug(msg: String, e: Throwable?) {
        logger.log(Level.FINE, msg, e)
    }

    override fun isInfoEnabled(): Boolean {
        return logger.isLoggable(Level.INFO)
    }

    override fun info(msg: String, e: Throwable?) {
        logger.log(Level.INFO, msg, e)
    }

    override fun isWarnEnabled(): Boolean {
        return logger.isLoggable(Level.WARNING)
    }

    override fun warn(msg: String, e: Throwable?) {
        logger.log(Level.WARNING, msg, e)
    }

    override fun isErrorEnabled(): Boolean {
        return logger.isLoggable(Level.SEVERE)
    }

    override fun error(msg: String, e: Throwable?) {
        logger.log(Level.SEVERE, msg, e)
    }
}

class Slf4jLoggerAdapter(val logger: org.slf4j.Logger) : Logger {

    override fun isTraceEnabled(): Boolean {
        return logger.isTraceEnabled
    }

    override fun trace(msg: String, e: Throwable?) {
        logger.trace(msg, e)
    }

    override fun isDebugEnabled(): Boolean {
        return logger.isDebugEnabled
    }

    override fun debug(msg: String, e: Throwable?) {
        logger.debug(msg, e)
    }

    override fun isInfoEnabled(): Boolean {
        return logger.isInfoEnabled
    }

    override fun info(msg: String, e: Throwable?) {
        logger.info(msg, e)
    }

    override fun isWarnEnabled(): Boolean {
        return logger.isWarnEnabled
    }

    override fun warn(msg: String, e: Throwable?) {
        logger.warn(msg, e)
    }

    override fun isErrorEnabled(): Boolean {
        return logger.isErrorEnabled
    }

    override fun error(msg: String, e: Throwable?) {
        logger.error(msg, e)
    }
}

class CommonsLoggerAdapter(val logger: org.apache.commons.logging.Log) : Logger {

    override fun isTraceEnabled(): Boolean {
        return logger.isTraceEnabled
    }

    override fun trace(msg: String, e: Throwable?) {
        logger.trace(msg, e)
    }

    override fun isDebugEnabled(): Boolean {
        return logger.isDebugEnabled
    }

    override fun debug(msg: String, e: Throwable?) {
        logger.debug(msg, e)
    }

    override fun isInfoEnabled(): Boolean {
        return logger.isInfoEnabled
    }

    override fun info(msg: String, e: Throwable?) {
        logger.info(msg, e)
    }

    override fun isWarnEnabled(): Boolean {
        return logger.isWarnEnabled
    }

    override fun warn(msg: String, e: Throwable?) {
        logger.warn(msg, e)
    }

    override fun isErrorEnabled(): Boolean {
        return logger.isErrorEnabled
    }

    override fun error(msg: String, e: Throwable?) {
        logger.error(msg, e)
    }
}