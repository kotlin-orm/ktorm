package me.liuwj.ktorm.logging

/**
 * Created by vince on May 05, 2019.
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