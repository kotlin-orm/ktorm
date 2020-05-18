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

package me.liuwj.ktorm.schema

import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by vince at May 01, 2020.
 */
internal object RefCounterContext {
    private val threadLocal = ThreadLocal<AtomicInteger>()

    internal fun setCounter(counter: AtomicInteger) {
        if (threadLocal.get() != null) {
            throw IllegalStateException("The counter is already set.")
        }

        threadLocal.set(counter)
    }

    internal fun getCounter(): AtomicInteger? {
        val counter = threadLocal.get()
        threadLocal.remove()
        return counter
    }
}
