package me.liuwj.ktorm.logging

/**
 * Adapter [Logger] implementation integrating Slf4j with Ktorm.
 *
 * @property logger a logger instance of Slf4j.
 */
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
