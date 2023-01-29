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
 * Adapter [Logger] implementation integrating
 * [android.util.Log](https://developer.android.com/reference/android/util/Log) with Ktorm.
 */
public class AndroidLoggerAdapter(private val tag: String) : Logger {
    // Access Android Log API by reflection, because Android SDK is not a JDK 9 module,
    // we are not able to require it in module-info.java.
    private val logClass = Class.forName("android.util.Log")
    private val isLoggableMethod = logClass.getMethod("isLoggable", String::class.java, Int::class.javaPrimitiveType)
    private val vMethod = logClass.getMethod("v", String::class.java, String::class.java, Throwable::class.java)
    private val dMethod = logClass.getMethod("d", String::class.java, String::class.java, Throwable::class.java)
    private val iMethod = logClass.getMethod("i", String::class.java, String::class.java, Throwable::class.java)
    private val wMethod = logClass.getMethod("w", String::class.java, String::class.java, Throwable::class.java)
    private val eMethod = logClass.getMethod("e", String::class.java, String::class.java, Throwable::class.java)

    private object Levels {
        const val VERBOSE = 2
        const val DEBUG = 3
        const val INFO = 4
        const val WARN = 5
        const val ERROR = 6
    }

    override fun isTraceEnabled(): Boolean {
        return isLoggableMethod.invoke0(null, tag, Levels.VERBOSE) as Boolean
    }

    override fun trace(msg: String, e: Throwable?) {
        vMethod.invoke0(null, tag, msg, e)
    }

    override fun isDebugEnabled(): Boolean {
        return isLoggableMethod.invoke0(null, tag, Levels.DEBUG) as Boolean
    }

    override fun debug(msg: String, e: Throwable?) {
        dMethod.invoke0(null, tag, msg, e)
    }

    override fun isInfoEnabled(): Boolean {
        return isLoggableMethod.invoke0(null, tag, Levels.INFO) as Boolean
    }

    override fun info(msg: String, e: Throwable?) {
        iMethod.invoke0(null, tag, msg, e)
    }

    override fun isWarnEnabled(): Boolean {
        return isLoggableMethod.invoke0(null, tag, Levels.WARN) as Boolean
    }

    override fun warn(msg: String, e: Throwable?) {
        wMethod.invoke0(null, tag, msg, e)
    }

    override fun isErrorEnabled(): Boolean {
        return isLoggableMethod.invoke0(null, tag, Levels.ERROR) as Boolean
    }

    override fun error(msg: String, e: Throwable?) {
        eMethod.invoke0(null, tag, msg, e)
    }
}
