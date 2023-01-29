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

package org.ktorm.support.mysql

import org.ktorm.dsl.Query
import org.ktorm.entity.EntitySequence
import org.ktorm.expression.SelectExpression
import org.ktorm.expression.TableExpression
import org.ktorm.expression.UnionExpression
import org.ktorm.schema.BaseTable
import org.ktorm.support.mysql.LockingMode.*
import org.ktorm.support.mysql.LockingWait.*

/**
 * MySQL locking mode.
 *
 * @since 3.4.0
 */
public enum class LockingMode {
    FOR_UPDATE,
    FOR_SHARE,
    LOCK_IN_SHARE_MODE
}

/**
 * MySQL wait strategy for locked records.
 *
 * @since 3.4.0
 */
public enum class LockingWait {
    WAIT,
    NOWAIT,
    SKIP_LOCKED
}

/**
 * MySQL locking clause, See https://dev.mysql.com/doc/refman/8.0/en/innodb-locking-reads.html
 *
 * @since 3.4.0
 */
public data class LockingClause(
    val mode: LockingMode,
    val tables: List<TableExpression>,
    val wait: LockingWait
)

/**
 * Specify the locking clause of this query, an example generated SQL could be:
 *
 * `select ... for update of table_name nowait`
 *
 * @param mode locking mode, one of [FOR_UPDATE], [FOR_SHARE], [LOCK_IN_SHARE_MODE].
 * @param tables specific the tables, only rows coming from those tables would be locked.
 * @param wait waiting strategy, one of [WAIT], [NOWAIT], [SKIP_LOCKED].
 * @since 3.4.0
 */
public fun Query.locking(
    mode: LockingMode, tables: List<BaseTable<*>> = emptyList(), wait: LockingWait = WAIT
): Query {
    val locking = LockingClause(mode, tables.map { it.asExpression() }, wait)

    val expr = when (val e = this.expression) {
        is SelectExpression -> e.copy(extraProperties = e.extraProperties + Pair("locking", locking))
        is UnionExpression -> throw IllegalStateException("Locking clause is not supported for a union expression.")
    }

    return this.withExpression(expr)
}

/**
 * Specify the locking clause of this query, an example generated SQL could be:
 *
 * `select ... for update of table_name nowait`
 *
 * @param mode locking mode, one of [FOR_UPDATE], [FOR_SHARE], [LOCK_IN_SHARE_MODE].
 * @param tables specific the tables, only rows coming from those tables would be locked.
 * @param wait waiting strategy, one of [WAIT], [NOWAIT], [SKIP_LOCKED].
 * @since 3.4.0
 */
public fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.locking(
    mode: LockingMode, tables: List<BaseTable<*>> = emptyList(), wait: LockingWait = WAIT
): EntitySequence<E, T> {
    val locking = LockingClause(mode, tables.map { it.asExpression() }, wait)

    return this.withExpression(
        expression.copy(extraProperties = expression.extraProperties + Pair("locking", locking))
    )
}
