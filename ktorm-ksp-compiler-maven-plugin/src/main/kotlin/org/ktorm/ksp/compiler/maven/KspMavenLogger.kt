package org.ktorm.ksp.compiler.maven

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.NonExistLocation
import org.apache.maven.plugin.logging.Log

internal class KspMavenLogger(val log: Log) : KSPLogger {

    override fun logging(message: String, symbol: KSNode?) {
        if (log.isDebugEnabled) {
            log.debug(format(message, symbol))
        }
    }

    override fun info(message: String, symbol: KSNode?) {
        if (log.isInfoEnabled) {
            log.info(format(message, symbol))
        }
    }

    override fun warn(message: String, symbol: KSNode?) {
        if (log.isWarnEnabled) {
            log.warn(format(message, symbol))
        }
    }

    override fun error(message: String, symbol: KSNode?) {
        if (log.isErrorEnabled) {
            log.error(format(message, symbol))
        }
    }

    override fun exception(e: Throwable) {
        if (log.isErrorEnabled) {
            log.error(e)
        }
    }

    private fun format(message: String, symbol: KSNode?) =
        when (val location = symbol?.location) {
            is FileLocation -> "[${location.filePath}:${location.lineNumber}] $message"
            is NonExistLocation, null -> message
        }
}
