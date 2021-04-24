package org.ktorm.support.mysql

import org.ktorm.dsl.Query
import org.ktorm.entity.EntitySequence
import org.ktorm.expression.SelectExpression
import org.ktorm.expression.UnionExpression
import org.ktorm.schema.BaseTable

internal enum class LockMode {
    FOR_UPDATE, FOR_SHARE
}

/**
 * Indicate that this query should acquire the record-lock,
 * the generated SQL would be `select ... for update`.
 *
 * @since 3.4.0
 */
public fun Query.forUpdate(): Query {
    val expr = when (val e = this.expression) {
        is SelectExpression -> e.copy(extraProperties = e.extraProperties + Pair("lockMode", LockMode.FOR_UPDATE))
        is UnionExpression -> throw IllegalStateException("forUpdate() is not supported in a union expression.")
    }

    return this.withExpression(expr)
}

/**
 * Indicate that this query should acquire the record-lock,
 * the generated SQL would be `select ... for update`.
 *
 * @since 3.4.0
 */
public fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.forUpdate(): EntitySequence<E, T> {
    return this.withExpression(
        expression.copy(extraProperties = expression.extraProperties + Pair("lockMode", LockMode.FOR_UPDATE))
    )
}

/**
 * Indicate that this query should acquire the record-lock in share mode,
 * the generated SQL would be `select ... lock in share mode`.
 *
 * @since 3.4.0
 */
public fun Query.lockInShareMode(): Query {
    val expr = when (val e = this.expression) {
        is SelectExpression -> e.copy(extraProperties = e.extraProperties + Pair("lockMode", LockMode.FOR_SHARE))
        is UnionExpression -> throw IllegalStateException("lockInShareMode() is not supported in a union expression.")
    }

    return this.withExpression(expr)
}

/**
 * Indicate that this query should acquire the record-lock in share mode,
 * the generated SQL would be `select ... lock in share mode`.
 *
 * @since 3.4.0
 */
public fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.lockInShareMode(): EntitySequence<E, T> {
    return this.withExpression(
        expression.copy(extraProperties = expression.extraProperties + Pair("lockMode", LockMode.FOR_SHARE))
    )
}
