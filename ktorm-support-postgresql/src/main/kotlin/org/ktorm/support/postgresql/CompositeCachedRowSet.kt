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

package org.ktorm.support.postgresql

import org.ktorm.database.CachedRowSet
import java.util.*

/**
 * To document.
 */
public class CompositeCachedRowSet {
    private val resultSets = LinkedList<CachedRowSet>()

    /**
     * To document.
     * @param rs todo
     * @return todo
     */
    public fun add(rs: CachedRowSet) {
        resultSets.add(rs)
    }

    /**
     * To document.
     */
    @Suppress("IteratorHasNextCallsNextMethod")
    public operator fun iterator(): Iterator<CachedRowSet> = object : Iterator<CachedRowSet> {
        private var cursor = 0
        private var hasNext: Boolean? = null

        override fun hasNext(): Boolean {
            val hasNext = (cursor < resultSets.size && resultSets[cursor].next()).also { hasNext = it }

            if (!hasNext) {
                return ++cursor < resultSets.size && hasNext()
            }

            return hasNext
        }

        override fun next(): CachedRowSet {
            return if (hasNext ?: hasNext()) {
                resultSets[cursor].also { hasNext = null }
            } else {
                throw NoSuchElementException()
            }
        }
    }

    /**
     * To document.
     */
    public fun asIterable(): Iterable<CachedRowSet> {
        return Iterable { iterator() }
    }
}
