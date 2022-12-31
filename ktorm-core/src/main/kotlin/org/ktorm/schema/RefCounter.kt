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

package org.ktorm.schema

/**
 * Created by vince at May 01, 2020.
 */
internal class RefCounter private constructor() {
    private var count = 0

    fun get(): Int {
        return count
    }

    fun getAndIncrement(): Int {
        return count++
    }

    companion object {
        private val threadLocal = ThreadLocal<RefCounter>()

        fun setContextCounter(counter: RefCounter) {
            if (threadLocal.get() != null) {
                throw IllegalStateException("The context counter is already set.")
            }

            threadLocal.set(counter)
        }

        fun getCounter(): RefCounter {
            val counter = threadLocal.get() ?: return RefCounter()
            threadLocal.remove()
            return counter
        }
    }
}
