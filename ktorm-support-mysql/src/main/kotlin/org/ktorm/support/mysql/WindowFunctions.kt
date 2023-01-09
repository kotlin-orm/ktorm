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

import org.ktorm.expression.AggregateExpression
import org.ktorm.expression.ArgumentExpression
import org.ktorm.expression.FrameExpression
import org.ktorm.expression.FrameExtentType
import org.ktorm.expression.FrameUnitType
import org.ktorm.expression.OrderByExpression
import org.ktorm.expression.WindowSpecificationExpression
import org.ktorm.expression.WindowFunctionExpression
import org.ktorm.expression.WindowFunctionType
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.SqlType

/**
 * Most MySQL aggregate functions also can be used as window functions.
 */
public infix fun <T : Any> AggregateExpression<T>.over(window: WindowSpecificationExpression): WindowFunctionExpression<T> {
    val arguments = if (this.argument != null) {
        listOf(this.argument!!)
    } else {
        emptyList()
    }
    return WindowFunctionExpression(
        WindowFunctionType.valueOf(this.type.name),
        arguments,
        window,
        this.sqlType
    )
}

/**
 * Specify the window specification.
 */
public infix fun <T : Any> WindowFunctionExpression<T>.over(window: WindowSpecificationExpression): WindowFunctionExpression<T> {
    return WindowFunctionExpression(
        functionName,
        arguments,
        window,
        this.sqlType
    )
}

/**
 * Specify the window partition-by clause.
 */
public fun partitionBy(vararg columns: ColumnDeclaring<*>): WindowSpecificationExpression {
    return WindowSpecificationExpression(
        partitionArguments = columns.map { it.asExpression() },
        orderByExpressions = emptyList(),
        frameUnit = null,
        frameExpression = null,
    )
}

/**
 * Specify the window order-by clause.
 */
public fun orderBy(vararg orderByExpression: OrderByExpression): WindowSpecificationExpression {
    return WindowSpecificationExpression(
        partitionArguments = emptyList(),
        orderByExpressions = orderByExpression.asList(),
        frameUnit = null,
        frameExpression = null,
    )
}

/**
 * Specify the window order-by clause.
 */
public fun WindowSpecificationExpression.orderBy(vararg orderByExpression: OrderByExpression): WindowSpecificationExpression {
    return WindowSpecificationExpression(
        partitionArguments,
        orderByExpression.asList(),
        null,
        null
    )
}

/**
 * Specify the window frame by `N preceding`.
 */
public fun Int.preceding(): FrameExpression<Int> {
    return FrameExpression(
        frameExtentType = FrameExtentType.PRECEDING,
        argument = ArgumentExpression(
            value = this,
            sqlType = SqlType.of()!!
        ),
        sqlType = SqlType.of()!!
    )
}

/**
 * Specify the window frame by `N following`.
 */
public fun Int.following(): FrameExpression<Int> {
    return FrameExpression(
        frameExtentType = FrameExtentType.FOLLOWING,
        argument = ArgumentExpression(
            value = this,
            sqlType = SqlType.of()!!
        ),
        sqlType = SqlType.of()!!
    )
}

/**
 * Translated to MySQL reserved keyword `UNBOUNDED PRECEDING`.
 */
public val UNBOUNDED_PRECEDING: FrameExpression<Int> = FrameExpression(
    frameExtentType = FrameExtentType.UNBOUNDED_PRECEDING,
    argument = null,
    sqlType = SqlType.of()!!
)

/**
 * Translated to MySQL reserved keyword `UNBOUNDED FOLLOWING`.
 */
public val UNBOUNDED_FOLLOWING: FrameExpression<Int> = FrameExpression(
    frameExtentType = FrameExtentType.UNBOUNDED_FOLLOWING,
    argument = null,
    sqlType = SqlType.of()!!
)

/**
 * Translated to MySQL reserved key word `CURRENT ROW`.
 */
public val CURRENT_ROW: FrameExpression<Int> = FrameExpression(
    frameExtentType = FrameExtentType.CURRENT_ROW,
    argument = null,
    sqlType = SqlType.of()!!
)

/**
 * Translated to MySQL frame unit `rows between`.
 */
public fun <A : Any, B : Any> WindowSpecificationExpression.rowsBetween(
    left: FrameExpression<A>,
    right: FrameExpression<B>
): WindowSpecificationExpression {
    return WindowSpecificationExpression(
        partitionArguments,
        orderByExpressions,
        FrameUnitType.ROWS_BETWEEN,
        Pair(left, right)
    )
}

/**
 * Translated to MySQL frame unit `range between`.
 */
public fun <A : Any, B : Any> WindowSpecificationExpression.rangeBetween(
    left: FrameExpression<A>,
    right: FrameExpression<B>
): WindowSpecificationExpression {
    return WindowSpecificationExpression(
        partitionArguments,
        orderByExpressions,
        FrameUnitType.RANGE_BETWEEN,
        Pair(left, right)
    )
}

/**
 * Translated to MySQL frame unit `range`.
 */
public fun <T : Any> WindowSpecificationExpression.range(
    frameExpression: FrameExpression<T>,
): WindowSpecificationExpression {
    return WindowSpecificationExpression(
        partitionArguments,
        orderByExpressions,
        FrameUnitType.RANGE,
        Pair(frameExpression, null)
    )
}

/**
 * Translated to MySQL frame unit `rows`.
 */
public fun <T : Any> WindowSpecificationExpression.row(
    frameExpression: FrameExpression<T>,
): WindowSpecificationExpression {
    return WindowSpecificationExpression(
        partitionArguments,
        orderByExpressions,
        FrameUnitType.ROWS,
        Pair(frameExpression, null)
    )
}

/**
 * MySQL rank window function, translated to `rank()`.
 */
public fun rank(): WindowFunctionExpression<Int> {
    return WindowFunctionExpression(
        functionName = WindowFunctionType.RANK,
        arguments = emptyList(),
        window = null,
        sqlType = SqlType.of()!!
    )
}

/**
 * MySQL row_number window function, translated to `row_number()`.
 */
public fun rowNumber(): WindowFunctionExpression<Int> {
    return WindowFunctionExpression(
        functionName = WindowFunctionType.ROW_NUMBER,
        arguments = emptyList(),
        window = null,
        sqlType = SqlType.of()!!
    )
}

/**
 * MySQL dense_rank window function, translated to `dense_rank()`.
 */
public fun denseRank(): WindowFunctionExpression<Int> {
    return WindowFunctionExpression(
        functionName = WindowFunctionType.DENSE_RANK,
        arguments = emptyList(),
        window = null,
        sqlType = SqlType.of()!!
    )
}

/**
 * MySQL percent_rank window function, translated to `percent_rank()`.
 */
public fun percentRank(): WindowFunctionExpression<Double> {
    return WindowFunctionExpression(
        functionName = WindowFunctionType.PERCENT_RANK,
        arguments = emptyList(),
        window = null,
        sqlType = SqlType.of()!!
    )
}

/**
 * MySQL cume_dist window function, translated to `cume_dist()`.
 */
public fun cumeDist(): WindowFunctionExpression<Int> {
    return WindowFunctionExpression(
        functionName = WindowFunctionType.CUME_DIST,
        arguments = emptyList(),
        window = null,
        sqlType = SqlType.of()!!
    )
}

/**
 * MySQL first_value window function, translated to `first_value(column)`.
 */
public fun <T : Any> firstValue(column: ColumnDeclaring<T>): WindowFunctionExpression<T> {
    return WindowFunctionExpression(
        functionName = WindowFunctionType.FIRST_VALUE,
        arguments = listOf(column.asExpression()),
        window = null,
        sqlType = column.sqlType
    )
}

/**
 * MySQL last_value window function, translated to `last_value(column)`.
 */
public fun <T : Any> lastValue(column: ColumnDeclaring<T>): WindowFunctionExpression<T> {
    return WindowFunctionExpression(
        functionName = WindowFunctionType.LAST_VALUE,
        arguments = listOf(column.asExpression()),
        window = null,
        sqlType = column.sqlType
    )
}

/**
 * MySQL ntile window function, translated to `ntile(n)`.
 */
public fun ntile(n: Int): WindowFunctionExpression<Int> {
    return WindowFunctionExpression(
        functionName = WindowFunctionType.NTILE,
        arguments = listOf(
            ArgumentExpression(
                n,
                SqlType.of() ?: error("Cannot detect the param's SqlType, please specify manually.")
            )
        ),
        window = null,
        sqlType = SqlType.of()!!
    )
}

/**
 * MySQL nth_value window function, translated to `nth_value(column, n)`.
 */
public fun <T : Any> nthValue(column: ColumnDeclaring<T>, n: Int): WindowFunctionExpression<T> {
    return WindowFunctionExpression(
        functionName = WindowFunctionType.NTH_VALUE,
        arguments = listOf(
            column.asExpression(),
            ArgumentExpression(n, SqlType.of() ?: error("Cannot detect the param's SqlType, please specify manually."))
        ),
        window = null,
        sqlType = column.sqlType
    )
}

/**
 * MySQL lead window function, translated to `lead(column, offset, <defaultValue>)`.
 */
public fun <T : Any> lead(
    column: ColumnDeclaring<T>,
    offset: Int,
    defaultValue: Int? = null
): WindowFunctionExpression<T> {
    val arguments = mutableListOf(
        column.asExpression(),
        ArgumentExpression(offset, SqlType.of() ?: error("Cannot detect the param's SqlType, please specify manually."))
    )
    if (defaultValue != null) {
        arguments.add(
            ArgumentExpression(
                defaultValue,
                SqlType.of() ?: error("Cannot detect the param's SqlType, please specify manually.")
            )
        )
    }

    return WindowFunctionExpression(
        functionName = WindowFunctionType.LEAD,
        arguments = arguments,
        window = null,
        sqlType = column.sqlType
    )
}

/**
 * MySQL lag window function, translated to `lag(column, offset, <defaultValue>)`.
 */
public fun <T : Any> lag(
    column: ColumnDeclaring<T>,
    offset: Int,
    defaultValue: Int? = null
): WindowFunctionExpression<T> {
    val arguments = mutableListOf(
        column.asExpression(),
        ArgumentExpression(offset, SqlType.of() ?: error("Cannot detect the param's SqlType, please specify manually."))
    )
    if (defaultValue != null) {
        arguments.add(
            ArgumentExpression(
                defaultValue,
                SqlType.of() ?: error("Cannot detect the param's SqlType, please specify manually.")
            )
        )
    }

    return WindowFunctionExpression(
        functionName = WindowFunctionType.LAG,
        arguments = arguments,
        window = null,
        sqlType = column.sqlType
    )
}
