/*
 * Copyright 2018-2021 the original author or authors.
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

import android.util.Log

/**
 * Adapter [Logger] implementation integrating
 * [android.util.Log](https://developer.android.com/reference/android/util/Log) with Ktorm.
 *
 * @property tag the tag for android logging.
 */
public class AndroidLoggerAdapter(public val tag: String) : Logger {

    override fun isTraceEnabled(): Boolean {
        return Log.isLoggable(tag, Log.VERBOSE)
    }

    override fun trace(msg: String, e: Throwable?) {
        Log.v(tag, msg, e)
    }

    override fun isDebugEnabled(): Boolean {
        return Log.isLoggable(tag, Log.DEBUG)
    }

    override fun debug(msg: String, e: Throwable?) {
        Log.d(tag, msg, e)
    }

    override fun isInfoEnabled(): Boolean {
        return Log.isLoggable(tag, Log.INFO)
    }

    override fun info(msg: String, e: Throwable?) {
        Log.i(tag, msg, e)
    }

    override fun isWarnEnabled(): Boolean {
        return Log.isLoggable(tag, Log.WARN)
    }

    override fun warn(msg: String, e: Throwable?) {
        Log.w(tag, msg, e)
    }

    override fun isErrorEnabled(): Boolean {
        return Log.isLoggable(tag, Log.ERROR)
    }

    override fun error(msg: String, e: Throwable?) {
        Log.e(tag, msg, e)
    }
}
