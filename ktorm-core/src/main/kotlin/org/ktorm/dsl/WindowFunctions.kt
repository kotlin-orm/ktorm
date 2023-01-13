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
 *
 * Return the number of the current row within its partition. Row numbers range from 1 to the number of partition rows.
 * ORDER BY affects the order in which rows are numbered. Without ORDER BY, row numbering is nondeterministic.
 *
 * @since 3.6.0
 */
public fun rowNumber(): WindowFunctionExpression<Int> {
    return WindowFunctionExpression(WindowFunctionType.ROW_NUMBER, emptyList(), sqlType = IntSqlType)
}

/**
 * The rank window function, translated to `rank()` in SQL.
 *
 * Return the rank of the current row within its partition, with gaps. Peers are considered ties and receive the same
 * rank. This function does not assign consecutive ranks to peer groups if groups of size greater than one exist;
 * the result is non-contiguous rank numbers.
 *
 * This function should be used with ORDER BY to sort partition rows into the desired order. Without ORDER BY,
 * all rows are peers.
 *
 * @since 3.6.0
 */
public fun rank(): WindowFunctionExpression<Int> {
    return WindowFunctionExpression(WindowFunctionType.RANK, emptyList(), sqlType = IntSqlType)
}

/**
 * The dense_rank window function, translated to `dense_rank()` in SQL.
 *
 * Return the rank of the current row within its partition, without gaps. Peers are considered ties and receive
 * the same rank. This function assigns consecutive ranks to peer groups; the result is that groups of size
 * greater than one do not produce non-contiguous rank numbers.
 *
 * This function should be used with ORDER BY to sort partition rows into the desired order. Without ORDER BY,
 * all rows are peers.
 *
 * @since 3.6.0
 */
public fun denseRank(): WindowFunctionExpression<Int> {
    return WindowFunctionExpression(WindowFunctionType.DENSE_RANK, emptyList(), sqlType = IntSqlType)
}

/**
 * The percent_rank window function, translated to `percent_rank()` in SQL.
 *
 * Return the percentage of partition values less than the value in the current row, excluding the highest value.
 * Return values range from 0 to 1 and represent the row relative rank, calculated as the result of this formula,
 * where rank is the row rank and rows is the number of partition rows: (rank - 1) / (rows - 1)
 *
 * This function should be used with ORDER BY to sort partition rows into the desired order. Without ORDER BY,
 * all rows are peers.
 *
 * @since 3.6.0
 */
public fun percentRank(): WindowFunctionExpression<Double> {
    return WindowFunctionExpression(WindowFunctionType.PERCENT_RANK, emptyList(), sqlType = DoubleSqlType)
}

/**
 * The cume_dist window function, translated to `cume_dist()` in SQL.
 *
 * Return the cumulative distribution of a value within a group of values; that is, the percentage of partition values
 * less than or equal to the value in the current row. This represents the number of rows preceding or peer with the
 * current row in the window ordering of the window partition divided by the total number of rows in the partition.
 * Return values range from 0 to 1.
 *
 * This function should be used with ORDER BY to sort partition rows into the desired order. Without ORDER BY,
 * all rows are peers and have value N/N = 1, where N is the partition size.
 *
 * @since 3.6.0
 */
public fun cumeDist(): WindowFunctionExpression<Double> {
    return WindowFunctionExpression(WindowFunctionType.CUME_DIST, emptyList(), sqlType = DoubleSqlType)
}

/**
 * The lag window function, translated to `lag(expr, offset[, defVal])` in SQL.
 *
 * Return the value of [expr] from the row that lags (precedes) the current row by [offset] rows within its partition.
 * If there is no such row, the return value is [defVal]. For example, if offset is 3, the return value is default
 * for the first three rows.
 *
 * @since 3.6.0
 */
public fun <T : Any> lag(
    expr: ColumnDeclaring<T>, offset: Int = 1, defVal: T? = null
): WindowFunctionExpression<T> {
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
 * The lag window function, translated to `lag(expr, offset[, defVal])` in SQL.
 *
 * Return the value of [expr] from the row that lags (precedes) the current row by [offset] rows within its partition.
 * If there is no such row, the return value is [defVal]. For example, if offset is 3, the return value is default
 * for the first three rows.
 *
 * @since 3.6.0
 */
public fun <T : Any> lag(
    expr: ColumnDeclaring<T>, offset: Int, defVal: ColumnDeclaring<T>
): WindowFunctionExpression<T> {
    return WindowFunctionExpression(
        type = WindowFunctionType.LAG,
        arguments = listOfNotNull(
            expr.asExpression(),
            ArgumentExpression(offset, IntSqlType),
            defVal.asExpression()
        ),
        sqlType = expr.sqlType
    )
}

/**
 * The lead window function, translated to `lead(expr, offset[, defVal])` in SQL.
 *
 * Return the value of [expr] from the row that leads (follows) the current row by [offset] rows within its partition.
 * If there is no such row, the return value is [defVal]. For example, if offset is 3, the return value is default
 * for the last three rows.
 *
 * @since 3.6.0
 */
public fun <T : Any> lead(
    expr: ColumnDeclaring<T>, offset: Int = 1, defVal: T? = null
): WindowFunctionExpression<T> {
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
 * The lead window function, translated to `lead(expr, offset[, defVal])` in SQL.
 *
 * Return the value of [expr] from the row that leads (follows) the current row by [offset] rows within its partition.
 * If there is no such row, the return value is [defVal]. For example, if offset is 3, the return value is default
 * for the last three rows.
 *
 * @since 3.6.0
 */
public fun <T : Any> lead(
    expr: ColumnDeclaring<T>, offset: Int, defVal: ColumnDeclaring<T>
): WindowFunctionExpression<T> {
    return WindowFunctionExpression(
        type = WindowFunctionType.LEAD,
        arguments = listOfNotNull(
            expr.asExpression(),
            ArgumentExpression(offset, IntSqlType),
            defVal.asExpression()
        ),
        sqlType = expr.sqlType
    )
}

/**
 * The first_value window function, translated to `first_value(expr)` in SQL.
 *
 * Return the value of [expr] from the first row of the window frame.
 *
 * @since 3.6.0
 */
public fun <T : Any> firstValue(expr: ColumnDeclaring<T>): WindowFunctionExpression<T> {
    return WindowFunctionExpression(WindowFunctionType.FIRST_VALUE, listOf(expr.asExpression()), sqlType = expr.sqlType)
}

/**
 * The last_value window function, translated to `last_value(expr)` in SQL.
 *
 * Return the value of [expr] from the last row of the window frame.
 *
 * @since 3.6.0
 */
public fun <T : Any> lastValue(expr: ColumnDeclaring<T>): WindowFunctionExpression<T> {
    return WindowFunctionExpression(WindowFunctionType.LAST_VALUE, listOf(expr.asExpression()), sqlType = expr.sqlType)
}

/**
 * The nth_value window function, translated to `nth_value(expr, n)` in SQL.
 *
 * Return the value of [expr] from the n-th row of the window frame. If there is no such row, the return value is null.
 *
 * @since 3.6.0
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
 *
 * Divide a partition into [n] groups (buckets), assigns each row in the partition its bucket number, and return
 * the bucket number of the current row within its partition. For example, if n is 4, ntile() divides rows into four
 * buckets. If n is 100, ntile() divides rows into 100 buckets.
 *
 * @since 3.6.0
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
 *
 * @since 3.6.0
 */
public fun <T : Any> WindowFunctionExpression<T>.over(
    window: WindowSpecificationExpression = window()
): WindowFunctionExpression<T> {
    return this.copy(window = window)
}

/**
 * Specify the window specification for this window function.
 *
 * @since 3.6.0
 */
public inline fun <T : Any> WindowFunctionExpression<T>.over(
    configure: WindowSpecificationExpression.() -> WindowSpecificationExpression
): WindowFunctionExpression<T> {
    return over(window().configure())
}

/**
 * Use this aggregate function as a window function and specify its window specification.
 *
 * @since 3.6.0
 */
public fun <T : Any> AggregateExpression<T>.over(
    window: WindowSpecificationExpression = window()
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
 * Use this aggregate function as a window function and specify its window specification.
 *
 * @since 3.6.0
 */
public inline fun <T : Any> AggregateExpression<T>.over(
    configure: WindowSpecificationExpression.() -> WindowSpecificationExpression
): WindowFunctionExpression<T> {
    return over(window().configure())
}

/**
 * Create a default window specification.
 *
 * @since 3.6.0
 */
public fun window(): WindowSpecificationExpression {
    return WindowSpecificationExpression()
}

/**
 * Specify the partition-by clause of this window using the given columns or expressions.
 *
 * A partition-by clause indicates how to divide the query rows into groups. The window function result for a given row
 * is based on the rows of the partition that contains the row. If partition-by is omitted, there is a single partition
 * consisting of all query rows.
 *
 * @since 3.6.0
 */
public fun WindowSpecificationExpression.partitionBy(
    columns: Collection<ColumnDeclaring<*>>
): WindowSpecificationExpression {
    return this.copy(partitionBy = columns.map { it.asExpression() })
}

/**
 * Specify the partition-by clause of this window using the given columns or expressions.
 *
 * A partition-by clause indicates how to divide the query rows into groups. The window function result for a given row
 * is based on the rows of the partition that contains the row. If partition-by is omitted, there is a single partition
 * consisting of all query rows.
 *
 * @since 3.6.0
 */
public fun WindowSpecificationExpression.partitionBy(
    vararg columns: ColumnDeclaring<*>
): WindowSpecificationExpression {
    return partitionBy(columns.asList())
}

/**
 * Specify the order-by clause of this window using the given order-by expressions.
 *
 * An order-by clause indicates how to sort rows in each partition. Partition rows that are equal according to the
 * order-by clause are considered peers. If order-by is omitted, partition rows are unordered, with no processing
 * order implied, and all partition rows are peers.
 *
 * @since 3.6.0
 */
public fun WindowSpecificationExpression.orderBy(orders: Collection<OrderByExpression>): WindowSpecificationExpression {
    return this.copy(orderBy = orders.toList())
}

/**
 * Specify the order-by clause of this window using the given order-by expressions.
 *
 * An order-by clause indicates how to sort rows in each partition. Partition rows that are equal according to the
 * order-by clause are considered peers. If order-by is omitted, partition rows are unordered, with no processing
 * order implied, and all partition rows are peers.
 *
 * @since 3.6.0
 */
public fun WindowSpecificationExpression.orderBy(vararg orders: OrderByExpression): WindowSpecificationExpression {
    return orderBy(orders.asList())
}

/**
 * Specify the frame clause of this window using the given bound in rows unit.
 *
 * With rows unit, the frame is defined by beginning and ending row positions. Offsets are differences in row numbers
 * from the current row number.
 *
 * @since 3.6.0
 */
public fun WindowSpecificationExpression.rows(bound: WindowFrameBoundExpression): WindowSpecificationExpression {
    return this.copy(frameUnit = WindowFrameUnitType.ROWS, frameStart = bound)
}

/**
 * Specify the frame clause of this window using the given bounds (start & end) in rows unit.
 *
 * With rows unit, the frame is defined by beginning and ending row positions. Offsets are differences in row numbers
 * from the current row number.
 *
 * @since 3.6.0
 */
public fun WindowSpecificationExpression.rowsBetween(
    start: WindowFrameBoundExpression, end: WindowFrameBoundExpression
): WindowSpecificationExpression {
    return this.copy(frameUnit = WindowFrameUnitType.ROWS, frameStart = start, frameEnd = end)
}

/**
 * Specify the frame clause of this window using the given bound in range unit.
 *
 * With range unit, the frame is defined by rows within a value range. Offsets are differences in row values
 * from the current row value.
 *
 * @since 3.6.0
 */
public fun WindowSpecificationExpression.range(bound: WindowFrameBoundExpression): WindowSpecificationExpression {
    return this.copy(frameUnit = WindowFrameUnitType.RANGE, frameStart = bound)
}

/**
 * Specify the frame clause of this window using the given bounds (start & end) in rows unit.
 *
 * With range unit, the frame is defined by rows within a value range. Offsets are differences in row values
 * from the current row value.
 *
 * @since 3.6.0
 */
public fun WindowSpecificationExpression.rangeBetween(
    start: WindowFrameBoundExpression, end: WindowFrameBoundExpression
): WindowSpecificationExpression {
    return this.copy(frameUnit = WindowFrameUnitType.RANGE, frameStart = start, frameEnd = end)
}

/**
 * Utility object that creates expressions of window frame bounds.
 *
 * @since 3.6.0
 */
public object WindowFrames {

    /**
     * Create a bound expression that represents `current row`.
     *
     * For ROWS, the bound is the current row. For RANGE, the bound is the peers of the current row.
     *
     * @since 3.6.0
     */
    public fun currentRow(): WindowFrameBoundExpression {
        return WindowFrameBoundExpression(WindowFrameBoundType.CURRENT_ROW, argument = null)
    }

    /**
     * Create a bound expression that represents `unbounded preceding`, which means the first partition row.
     *
     * @since 3.6.0
     */
    public fun unboundedPreceding(): WindowFrameBoundExpression {
        return WindowFrameBoundExpression(WindowFrameBoundType.UNBOUNDED_PRECEDING, argument = null)
    }

    /**
     * Create a bound expression that represents `unbounded following`, which means the last partition row.
     *
     * @since 3.6.0
     */
    public fun unboundedFollowing(): WindowFrameBoundExpression {
        return WindowFrameBoundExpression(WindowFrameBoundType.UNBOUNDED_FOLLOWING, argument = null)
    }

    /**
     * Create a bound expression that represents `N preceding`.
     *
     * For ROWS, the bound is [n] rows before the current row. For RANGE, the bound is the rows with values equal to
     * the current row value minus [n]; if the current row value is NULL, the bound is the peers of the row.
     *
     * @since 3.6.0
     */
    public inline fun <reified T : Number> preceding(
        n: T,
        sqlType: SqlType<T> = SqlType.of() ?: error("Cannot detect the argument's SqlType, please specify manually.")
    ): WindowFrameBoundExpression {
        return WindowFrameBoundExpression(WindowFrameBoundType.PRECEDING, ArgumentExpression(n, sqlType))
    }

    /**
     * Create a bound expression that represents `N following`.
     *
     * For ROWS, the bound is [n] rows after the current row. For RANGE, the bound is the rows with values equal to
     * the current row value plus [n]; if the current row value is NULL, the bound is the peers of the row.
     *
     * @since 3.6.0
     */
    public inline fun <reified T : Number> following(
        n: T,
        sqlType: SqlType<T> = SqlType.of() ?: error("Cannot detect the argument's SqlType, please specify manually.")
    ): WindowFrameBoundExpression {
        return WindowFrameBoundExpression(WindowFrameBoundType.FOLLOWING, ArgumentExpression(n, sqlType))
    }
}
