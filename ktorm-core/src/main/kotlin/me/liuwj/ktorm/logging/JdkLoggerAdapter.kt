package me.liuwj.ktorm.logging

import java.util.logging.Level

/**
 * Created by vince on May 05, 2019.
 */
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