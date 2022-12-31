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

import org.ktorm.entity.invoke0

/**
 * Adapter [Logger] implementation integrating Slf4j with Ktorm.
 */
public class Slf4jLoggerAdapter(loggerName: String) : Logger {
    // Access SLF4J API by reflection, because we haven't required it in module-info.java.
    private val loggerFactoryClass = Class.forName("org.slf4j.LoggerFactory")
    private val loggerClass = Class.forName("org.slf4j.Logger")
    private val getLoggerMethod = loggerFactoryClass.getMethod("getLogger", String::class.java)
    private val isTraceEnabledMethod = loggerClass.getMethod("isTraceEnabled")
    private val isDebugEnabledMethod = loggerClass.getMethod("isDebugEnabled")
    private val isInfoEnabledMethod = loggerClass.getMethod("isInfoEnabled")
    private val isWarnEnabledMethod = loggerClass.getMethod("isWarnEnabled")
    private val isErrorEnabledMethod = loggerClass.getMethod("isErrorEnabled")
    private val traceMethod = loggerClass.getMethod("trace", String::class.java, Throwable::class.java)
    private val debugMethod = loggerClass.getMethod("debug", String::class.java, Throwable::class.java)
    private val infoMethod = loggerClass.getMethod("info", String::class.java, Throwable::class.java)
    private val warnMethod = loggerClass.getMethod("warn", String::class.java, Throwable::class.java)
    private val errorMethod = loggerClass.getMethod("error", String::class.java, Throwable::class.java)
    private val logger = getLoggerMethod.invoke0(null, loggerName)

    override fun isTraceEnabled(): Boolean {
        return isTraceEnabledMethod.invoke0(logger) as Boolean
    }

    override fun trace(msg: String, e: Throwable?) {
        traceMethod.invoke0(logger, msg, e)
    }

    override fun isDebugEnabled(): Boolean {
        return isDebugEnabledMethod.invoke0(logger) as Boolean
    }

    override fun debug(msg: String, e: Throwable?) {
        debugMethod.invoke0(logger, msg, e)
    }

    override fun isInfoEnabled(): Boolean {
        return isInfoEnabledMethod.invoke0(logger) as Boolean
    }

    override fun info(msg: String, e: Throwable?) {
        infoMethod.invoke0(logger, msg, e)
    }

    override fun isWarnEnabled(): Boolean {
        return isWarnEnabledMethod.invoke0(logger) as Boolean
    }

    override fun warn(msg: String, e: Throwable?) {
        warnMethod.invoke0(logger, msg, e)
    }

    override fun isErrorEnabled(): Boolean {
        return isErrorEnabledMethod.invoke0(logger) as Boolean
    }

    override fun error(msg: String, e: Throwable?) {
        errorMethod.invoke0(logger, msg, e)
    }
}
