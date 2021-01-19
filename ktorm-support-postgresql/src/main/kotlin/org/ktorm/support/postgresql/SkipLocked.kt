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

import org.ktorm.dsl.Query
import org.ktorm.entity.EntitySequence
import org.ktorm.expression.SelectExpression
import org.ktorm.expression.UnionExpression
import org.ktorm.schema.BaseTable

internal const val SKIP_LOCKED = "skipLocked"

/**
 * Indicate that this query should skip locked records, the generated SQL would be `select ... skip locked`.
 */
public fun Query.skipLocked(): Query {
    val expr = when (expression) {
        is SelectExpression ->
            (expression as SelectExpression)
                .copy(extraProperties = expression.extraProperties + (SKIP_LOCKED to true))
        is UnionExpression ->
            throw IllegalStateException("SELECT SKIP LOCKS is not supported in a union expression.")
    }

    return this.withExpression(expr)
}

/**
 * Indicate that the generated query should skip locked records, the generated SQL would be `select ... skip locked`.
 */
public fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.skipLocked(): EntitySequence<E, T> {
    return this.withExpression(expression.copy(extraProperties = expression.extraProperties + (SKIP_LOCKED to true)))
}
