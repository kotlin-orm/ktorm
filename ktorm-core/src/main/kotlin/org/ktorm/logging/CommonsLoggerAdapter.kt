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
 * Adapter [Logger] implementation integrating Apache Commons Logging with Ktorm.
 */
public class CommonsLoggerAdapter(loggerName: String) : Logger {
    // Access commons logging API by reflection, because it is not a JDK 9 module,
    // we are not able to require it in module-info.java.
    private val logFactoryClass = Class.forName("org.apache.commons.logging.LogFactory")
    private val logClass = Class.forName("org.apache.commons.logging.Log")
    private val getLogMethod = logFactoryClass.getMethod("getLog", String::class.java)
    private val isTraceEnabledMethod = logClass.getMethod("isTraceEnabled")
    private val isDebugEnabledMethod = logClass.getMethod("isDebugEnabled")
    private val isInfoEnabledMethod = logClass.getMethod("isInfoEnabled")
    private val isWarnEnabledMethod = logClass.getMethod("isWarnEnabled")
    private val isErrorEnabledMethod = logClass.getMethod("isErrorEnabled")
    private val traceMethod = logClass.getMethod("trace", Any::class.java, Throwable::class.java)
    private val debugMethod = logClass.getMethod("debug", Any::class.java, Throwable::class.java)
    private val infoMethod = logClass.getMethod("info", Any::class.java, Throwable::class.java)
    private val warnMethod = logClass.getMethod("warn", Any::class.java, Throwable::class.java)
    private val errorMethod = logClass.getMethod("error", Any::class.java, Throwable::class.java)
    private val logger = getLogMethod.invoke0(null, loggerName)

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
