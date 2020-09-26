/*
 * Copyright 2018-2020 the original author or authors.
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

/**
 * Adapter [Logger] implementation integrating Apache Commons Logging with Ktorm.
 *
 * @property logger a logger instance of Apache Commons Logging.
 */
public class CommonsLoggerAdapter(public val logger: org.apache.commons.logging.Log) : Logger {

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
