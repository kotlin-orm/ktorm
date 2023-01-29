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
 * [Logger] implementation that performs no operations.
 */
public object NoOpLogger : Logger {

    override fun isTraceEnabled(): Boolean {
        return false
    }

    override fun trace(msg: String, e: Throwable?) {
        // no-op
    }

    override fun isDebugEnabled(): Boolean {
        return false
    }

    override fun debug(msg: String, e: Throwable?) {
        // no-op
    }

    override fun isInfoEnabled(): Boolean {
        return false
    }

    override fun info(msg: String, e: Throwable?) {
        // no-op
    }

    override fun isWarnEnabled(): Boolean {
        return false
    }

    override fun warn(msg: String, e: Throwable?) {
        // no-op
    }

    override fun isErrorEnabled(): Boolean {
        return false
    }

    override fun error(msg: String, e: Throwable?) {
        // no-op
    }
}
