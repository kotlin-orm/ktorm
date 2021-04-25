package org.ktorm.support.postgresql

import org.ktorm.dsl.Query
import org.ktorm.entity.EntitySequence
import org.ktorm.expression.SelectExpression
import org.ktorm.expression.UnionExpression
import org.ktorm.schema.BaseTable
import org.ktorm.support.postgresql.LockingStrength.*
import org.ktorm.support.postgresql.LockingWait.*

/**
 * PostgreSQL lock strength.
 *
 * @since 3.4.0
 */
public enum class LockingStrength {
    FOR_UPDATE,
    FOR_NO_KEY_UPDATE,
    FOR_SHARE,
    FOR_KEY_SHARE
}

/**
 * PostgreSQL waiting strategy for locked records.
 *
 * @since 3.4.0
 */
public enum class LockingWait {
    BLOCK,
    NOWAIT,
    SKIP_LOCKED
}

/**
 * PostgreSQL locking clause.
 *
 * @since 3.4.0
 */
public data class LockingClause(
    val strength: LockingStrength,
    val tables: List<String>,
    val wait: LockingWait
)

/**
 * Specify the locking clause of this query, an example generated SQL could be:
 *
 * `select ... for update of table_name nowait`
 *
 * @param strength locking strength, one of [FOR_UPDATE], [FOR_NO_KEY_UPDATE], [FOR_SHARE], [FOR_KEY_SHARE].
 * @param tables specific the tables, then only rows coming from those tables would be locked.
 * @param wait waiting strategy, one of [BLOCK], [NOWAIT], [SKIP_LOCKED].
 * @since 3.4.0
 */
public fun Query.locking(
    strength: LockingStrength, tables: List<BaseTable<*>> = emptyList(), wait: LockingWait = BLOCK
): Query {
    val locking = LockingClause(strength, tables.map { it.tableName }, wait)

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
 * @param strength locking strength, one of [FOR_UPDATE], [FOR_NO_KEY_UPDATE], [FOR_SHARE], [FOR_KEY_SHARE].
 * @param tables specific the tables, then only rows coming from those tables would be locked.
 * @param wait waiting strategy, one of [BLOCK], [NOWAIT], [SKIP_LOCKED].
 * @since 3.4.0
 */
public fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.locking(
    strength: LockingStrength, tables: List<BaseTable<*>> = emptyList(), wait: LockingWait = BLOCK
): EntitySequence<E, T> {
    val locking = LockingClause(strength, tables.map { it.tableName }, wait)

    return this.withExpression(
        expression.copy(extraProperties = expression.extraProperties + Pair("locking", locking))
    )
}
