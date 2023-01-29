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

/**
 * Simple [Logger] implementation printing logs to the console. While messages at WARN or ERROR levels are printed to
 * [System.err], others are printed to [System.out].
 *
 * @property threshold a threshold controlling which log levels are enabled.
 */
public class ConsoleLogger(public val threshold: LogLevel) : Logger {

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

            if (e != null) {
                // Workaround for the compiler bug, see https://youtrack.jetbrains.com/issue/KT-34826
                @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "KotlinConstantConditions")
                (e as java.lang.Throwable).printStackTrace(out)
            }
        }
    }
}
