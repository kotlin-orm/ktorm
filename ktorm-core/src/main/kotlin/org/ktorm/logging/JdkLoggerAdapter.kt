/*
 * Copyright 2018-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ktorm.logging

import java.util.logging.Level

/**
 * Adapter [Logger] implementation integrating [java.util.logging] with Ktorm.
 */
public class JdkLoggerAdapter(loggerName: String) : Logger {
    private val logger = java.util.logging.Logger.getLogger(loggerName)

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
