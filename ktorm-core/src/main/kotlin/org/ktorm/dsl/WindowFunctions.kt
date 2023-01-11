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

import org.ktorm.expression.AggregateExpression
import org.ktorm.expression.ArgumentExpression
import org.ktorm.expression.OrderByExpression
import org.ktorm.expression.WindowSpecificationExpression
import org.ktorm.expression.WindowFunctionExpression
import org.ktorm.expression.WindowFunctionType
import org.ktorm.expression.WindowFunctionType.*
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.DoubleSqlType
import org.ktorm.schema.IntSqlType
import org.ktorm.schema.SqlType

/**
 * The row_number window function, translated to `row_number()` in SQL.
 */
public fun rowNumber(): WindowFunctionExpression<Int> {
    return WindowFunctionExpression(ROW_NUMBER, emptyList(), WindowSpecificationExpression(), IntSqlType)
}

/**
 * The rank window function, translated to `rank()` in SQL.
 */
public fun rank(): WindowFunctionExpression<Int> {
    return WindowFunctionExpression(RANK, emptyList(), WindowSpecificationExpression(), IntSqlType)
}

/**
 * The dense_rank window function, translated to `dense_rank()` in SQL.
 */
public fun denseRank(): WindowFunctionExpression<Int> {
    return WindowFunctionExpression(DENSE_RANK, emptyList(), WindowSpecificationExpression(), IntSqlType)
}

/**
 * The percent_rank window function, translated to `percent_rank()` in SQL.
 */
public fun percentRank(): WindowFunctionExpression<Double> {
    return WindowFunctionExpression(PERCENT_RANK, emptyList(), WindowSpecificationExpression(), DoubleSqlType)
}

/**
 * The cume_dist window function, translated to `cume_dist()` in SQL.
 */
public fun cumeDist(): WindowFunctionExpression<Double> {
    return WindowFunctionExpression(CUME_DIST, emptyList(), WindowSpecificationExpression(), DoubleSqlType)
}

/**
 * The lag window function, translated to `lag(expr, offset[, defVal])` in SQL.
 */
public fun <T : Any> lag(expr: ColumnDeclaring<T>, offset: Int = 1, defVal: T? = null): WindowFunctionExpression<T> {
    return WindowFunctionExpression(
        type = LAG,
        arguments = listOfNotNull(
            expr.asExpression(),
            ArgumentExpression(offset, IntSqlType),
            defVal?.let { ArgumentExpression(it, expr.sqlType) }
        ),
        window = WindowSpecificationExpression(),
        sqlType = expr.sqlType
    )
}

/**
 * The lead window function, translated to `lead(expr, offset[, defVal])` in SQL.
 */
public fun <T : Any> lead(expr: ColumnDeclaring<T>, offset: Int = 1, defVal: T? = null): WindowFunctionExpression<T> {
    return WindowFunctionExpression(
        type = LEAD,
        arguments = listOfNotNull(
            expr.asExpression(),
            ArgumentExpression(offset, IntSqlType),
            defVal?.let { ArgumentExpression(it, expr.sqlType) }
        ),
        window = WindowSpecificationExpression(),
        sqlType = expr.sqlType
    )
}

/**
 * The first_value window function, translated to `first_value(expr)` in SQL.
 */
public fun <T : Any> firstValue(expr: ColumnDeclaring<T>): WindowFunctionExpression<T> {
    return WindowFunctionExpression(
        type = FIRST_VALUE,
        arguments = listOf(expr.asExpression()),
        window = WindowSpecificationExpression(),
        sqlType = expr.sqlType
    )
}

/**
 * The last_value window function, translated to `last_value(expr)` in SQL.
 */
public fun <T : Any> lastValue(expr: ColumnDeclaring<T>): WindowFunctionExpression<T> {
    return WindowFunctionExpression(
        type = LAST_VALUE,
        arguments = listOf(expr.asExpression()),
        window = WindowSpecificationExpression(),
        sqlType = expr.sqlType
    )
}

/**
 * The nth_value window function, translated to `nth_value(expr, n)` in SQL.
 */
public fun <T : Any> nthValue(expr: ColumnDeclaring<T>, n: Int): WindowFunctionExpression<T> {
    return WindowFunctionExpression(
        type = NTH_VALUE,
        arguments = listOf(expr.asExpression(), ArgumentExpression(n, IntSqlType)),
        window = WindowSpecificationExpression(),
        sqlType = expr.sqlType
    )
}

/**
 * The ntile window function, translated to `ntile(n)` in SQL.
 */
public fun ntile(n: Int): WindowFunctionExpression<Int> {
    return WindowFunctionExpression(
        type = NTILE,
        arguments = listOf(ArgumentExpression(n, IntSqlType)),
        window = WindowSpecificationExpression(),
        sqlType = IntSqlType
    )
}











/**
 * Most MySQL aggregate functions also can be used as window functions.
 */
public infix fun <T : Any> AggregateExpression<T>.over(window: WindowSpecificationExpression): WindowFunctionExpression<T> {
    val arguments = if (this.argument != null) {
        listOf(this.argument)
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
