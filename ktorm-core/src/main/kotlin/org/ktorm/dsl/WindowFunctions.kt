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

package org.ktorm.dsl

import org.ktorm.expression.*
import org.ktorm.expression.WindowFunctionType
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.DoubleSqlType
import org.ktorm.schema.IntSqlType
import org.ktorm.schema.SqlType

/**
 * The row_number window function, translated to `row_number()` in SQL.
 */
public fun rowNumber(): WindowFunctionExpression<Int> {
    return WindowFunctionExpression(WindowFunctionType.ROW_NUMBER, emptyList(), sqlType = IntSqlType)
}

/**
 * The rank window function, translated to `rank()` in SQL.
 */
public fun rank(): WindowFunctionExpression<Int> {
    return WindowFunctionExpression(WindowFunctionType.RANK, emptyList(), sqlType = IntSqlType)
}

/**
 * The dense_rank window function, translated to `dense_rank()` in SQL.
 */
public fun denseRank(): WindowFunctionExpression<Int> {
    return WindowFunctionExpression(WindowFunctionType.DENSE_RANK, emptyList(), sqlType = IntSqlType)
}

/**
 * The percent_rank window function, translated to `percent_rank()` in SQL.
 */
public fun percentRank(): WindowFunctionExpression<Double> {
    return WindowFunctionExpression(WindowFunctionType.PERCENT_RANK, emptyList(), sqlType = DoubleSqlType)
}

/**
 * The cume_dist window function, translated to `cume_dist()` in SQL.
 */
public fun cumeDist(): WindowFunctionExpression<Double> {
    return WindowFunctionExpression(WindowFunctionType.CUME_DIST, emptyList(), sqlType = DoubleSqlType)
}

/**
 * The lag window function, translated to `lag(expr, offset[, defVal])` in SQL.
 */
public fun <T : Any> lag(expr: ColumnDeclaring<T>, offset: Int = 1, defVal: T? = null): WindowFunctionExpression<T> {
    return WindowFunctionExpression(
        type = WindowFunctionType.LAG,
        arguments = listOfNotNull(
            expr.asExpression(),
            ArgumentExpression(offset, IntSqlType),
            defVal?.let { ArgumentExpression(it, expr.sqlType) }
        ),
        sqlType = expr.sqlType
    )
}

/**
 * The lead window function, translated to `lead(expr, offset[, defVal])` in SQL.
 */
public fun <T : Any> lead(expr: ColumnDeclaring<T>, offset: Int = 1, defVal: T? = null): WindowFunctionExpression<T> {
    return WindowFunctionExpression(
        type = WindowFunctionType.LEAD,
        arguments = listOfNotNull(
            expr.asExpression(),
            ArgumentExpression(offset, IntSqlType),
            defVal?.let { ArgumentExpression(it, expr.sqlType) }
        ),
        sqlType = expr.sqlType
    )
}

/**
 * The first_value window function, translated to `first_value(expr)` in SQL.
 */
public fun <T : Any> firstValue(expr: ColumnDeclaring<T>): WindowFunctionExpression<T> {
    return WindowFunctionExpression(WindowFunctionType.FIRST_VALUE, listOf(expr.asExpression()), sqlType = expr.sqlType)
}

/**
 * The last_value window function, translated to `last_value(expr)` in SQL.
 */
public fun <T : Any> lastValue(expr: ColumnDeclaring<T>): WindowFunctionExpression<T> {
    return WindowFunctionExpression(WindowFunctionType.LAST_VALUE, listOf(expr.asExpression()), sqlType = expr.sqlType)
}

/**
 * The nth_value window function, translated to `nth_value(expr, n)` in SQL.
 */
public fun <T : Any> nthValue(expr: ColumnDeclaring<T>, n: Int): WindowFunctionExpression<T> {
    return WindowFunctionExpression(
        type = WindowFunctionType.NTH_VALUE,
        arguments = listOf(expr.asExpression(), ArgumentExpression(n, IntSqlType)),
        sqlType = expr.sqlType
    )
}

/**
 * The ntile window function, translated to `ntile(n)` in SQL.
 */
public fun ntile(n: Int): WindowFunctionExpression<Int> {
    return WindowFunctionExpression(
        type = WindowFunctionType.NTILE,
        arguments = listOf(ArgumentExpression(n, IntSqlType)),
        sqlType = IntSqlType
    )
}

/**
 * Specify the window specification for this window function.
 */
public infix fun <T : Any> WindowFunctionExpression<T>.over(
    window: WindowSpecificationExpression
): WindowFunctionExpression<T> {
    return this.copy(window = window)
}

/**
 * Use this aggregate function as a window function and specify its window specification.
 */
public infix fun <T : Any> AggregateExpression<T>.over(
    window: WindowSpecificationExpression
): WindowFunctionExpression<T> {
    return WindowFunctionExpression(
        type = WindowFunctionType.valueOf(this.type.name),
        arguments = listOfNotNull(this.argument),
        isDistinct = this.isDistinct,
        window = window,
        sqlType = this.sqlType
    )
}

/**
 * Create a default window specification.
 */
public fun window(): WindowSpecificationExpression {
    return WindowSpecificationExpression()
}

/**
 * Specify the partition-by clause of this window using the given columns or expressions.
 */
public fun WindowSpecificationExpression.partitionBy(
    columns: Collection<ColumnDeclaring<*>>
): WindowSpecificationExpression {
    return this.copy(partitionBy = columns.map { it.asExpression() })
}

/**
 * Specify the partition-by clause of this window using the given columns or expressions.
 */
public fun WindowSpecificationExpression.partitionBy(
    vararg columns: ColumnDeclaring<*>
): WindowSpecificationExpression {
    return partitionBy(columns.asList())
}

/**
 * Specify the order-by clause of this window using the given order-by expressions.
 */
public fun WindowSpecificationExpression.orderBy(orders: Collection<OrderByExpression>): WindowSpecificationExpression {
    return this.copy(orderBy = orders.toList())
}

/**
 * Specify the order-by clause of this window using the given order-by expressions.
 */
public fun WindowSpecificationExpression.orderBy(vararg orders: OrderByExpression): WindowSpecificationExpression {
    return orderBy(orders.asList())
}

/**
 * Specify the frame clause of this window using the given bound in rows unit.
 */
public fun WindowSpecificationExpression.rows(bound: WindowFrameBoundExpression): WindowSpecificationExpression {
    return this.copy(frameUnit = WindowFrameUnitType.ROWS, frameStart = bound)
}

/**
 * Specify the frame clause of this window using the given bounds (start & end) in rows unit.
 */
public fun WindowSpecificationExpression.rowsBetween(
    start: WindowFrameBoundExpression, end: WindowFrameBoundExpression
): WindowSpecificationExpression {
    return this.copy(frameUnit = WindowFrameUnitType.ROWS, frameStart = start, frameEnd = end)
}

/**
 * Specify the frame clause of this window using the given bound in range unit.
 */
public fun WindowSpecificationExpression.range(bound: WindowFrameBoundExpression): WindowSpecificationExpression {
    return this.copy(frameUnit = WindowFrameUnitType.RANGE, frameStart = bound)
}

/**
 * Specify the frame clause of this window using the given bounds (start & end) in rows unit.
 */
public fun WindowSpecificationExpression.rangeBetween(
    start: WindowFrameBoundExpression, end: WindowFrameBoundExpression
): WindowSpecificationExpression {
    return this.copy(frameUnit = WindowFrameUnitType.RANGE, frameStart = start, frameEnd = end)
}

/**
 * Utility object that creates expressions of window frame bounds.
 */
public object WindowFrames {

    /**
     * Create a bound expression that represents `current row`.
     */
    public fun currentRow(): WindowFrameBoundExpression {
        return WindowFrameBoundExpression(WindowFrameBoundType.CURRENT_ROW, argument = null)
    }

    /**
     * Create a bound expression that represents `unbounded preceding`.
     */
    public fun unboundedPreceding(): WindowFrameBoundExpression {
        return WindowFrameBoundExpression(WindowFrameBoundType.UNBOUNDED_PRECEDING, argument = null)
    }

    /**
     * Create a bound expression that represents `unbounded following`.
     */
    public fun unboundedFollowing(): WindowFrameBoundExpression {
        return WindowFrameBoundExpression(WindowFrameBoundType.UNBOUNDED_FOLLOWING, argument = null)
    }

    /**
     * Create a bound expression that represents `N preceding`.
     */
    public inline fun <reified T : Number> preceding(
        n: T,
        sqlType: SqlType<T> = SqlType.of() ?: error("Cannot detect the argument's SqlType, please specify manually.")
    ): WindowFrameBoundExpression {
        return WindowFrameBoundExpression(WindowFrameBoundType.PRECEDING, ArgumentExpression(n, sqlType))
    }

    /**
     * Create a bound expression that represents `N following`.
     */
    public inline fun <reified T : Number> following(
        n: T,
        sqlType: SqlType<T> = SqlType.of() ?: error("Cannot detect the argument's SqlType, please specify manually.")
    ): WindowFrameBoundExpression {
        return WindowFrameBoundExpression(WindowFrameBoundType.FOLLOWING, ArgumentExpression(n, sqlType))
    }
}
