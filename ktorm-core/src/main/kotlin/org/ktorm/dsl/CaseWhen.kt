/*
 * Copyright 2018-2022 the original author or authors.
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

import org.ktorm.expression.ArgumentExpression
import org.ktorm.expression.CaseWhenExpression
import org.ktorm.expression.ScalarExpression
import org.ktorm.schema.BooleanSqlType
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.SqlType

/**
 * case when of case Expression.
 * [caseValue] case value, may be null
 * [caseSqlType] case value sqlType
 *
 * if [caseValue] is null, then [caseSqlType] is [BooleanSqlType]
 */
public data class CaseExpression<T : Any> internal constructor(
    internal val caseValue: ScalarExpression<T>? = null,
    internal val caseSqlType: SqlType<T>,
)

/**
 * case when of when Expression.
 *
 * [caseValue] case value, may be null
 * [condition] when condition
 * [caseWhenExpression], the [CaseWhenExpression] when build [WhenExpression]
 * from [CaseWhenExpression], may be bull
 */
public data class WhenExpression<T : Any, V : Any> internal constructor(
    internal val caseValue: ScalarExpression<T>?,
    internal val condition: ScalarExpression<T>,
    internal val caseWhenExpression: CaseWhenExpression<T, V>? = null,
)

/**
 * DSL to build [CaseExpression] without [CaseExpression.caseValue].
 */
@Suppress("FunctionName")
public fun CASE(): CaseExpression<Boolean> {
    return CaseExpression(null, BooleanSqlType)
}

/**
 * DSL to build [CaseExpression] with [CaseExpression.caseValue].
 */
@Suppress("FunctionName")
public fun <T : Any> CASE(caseValue: ColumnDeclaring<T>): CaseExpression<T> {
    return CaseExpression(caseValue.asExpression(), caseValue.sqlType)
}

/**
 * DSL to build [WhenExpression] from [CaseExpression].
 */
@Suppress("FunctionName")
public infix fun <T : Any> CaseExpression<T>.WHEN(condition: ColumnDeclaring<T>): WhenExpression<T, Nothing> {
    return WhenExpression(
        caseValue = this.caseValue,
        condition = condition.asExpression(),
        caseWhenExpression = null
    )
}

/**
 * DSL to build [WhenExpression] from [CaseExpression].
 */
@Suppress("FunctionName")
public infix fun <T : Any> CaseExpression<T>.WHEN(condition: T): WhenExpression<T, Nothing> {
    return WhenExpression(
        this.caseValue,
        ArgumentExpression(condition, this.caseSqlType),
        caseWhenExpression = null
    )
}

/**
 * DSL to build [CaseWhenExpression] from [WhenExpression].
 *
 * [value] the value of then branch
 */
@JvmName("_THEN")
@Suppress("FunctionName")
public infix fun <T : Any, V : Any> WhenExpression<T, V>.THEN(value: V): CaseWhenExpression<T, V> {
    return this.caseWhenExpression!!.copy(
        whenThenConditions = this.caseWhenExpression.whenThenConditions +
            (this.condition to
                ArgumentExpression(
                    value,
                    this.caseWhenExpression.sqlType
                ))
    )
}

/**
 * DSL to build [CaseWhenExpression] from [WhenExpression].
 *
 * [value] the value of then branch
 */
@JvmName("_THEN")
@Suppress("FunctionName")
public infix fun <T : Any, V : Any> WhenExpression<T, V>.THEN(value: ColumnDeclaring<V>): CaseWhenExpression<T, V> {
    return this.caseWhenExpression!!.copy(
        whenThenConditions = this.caseWhenExpression.whenThenConditions +
            (this.condition to value.asExpression())
    )
}

/**
 * DSL to build [CaseWhenExpression] from [WhenExpression].
 *
 * [value] the value of then branch
 */
@Suppress("FunctionName")
public infix fun <T : Any, V : Any> WhenExpression<T, Nothing>.THEN(
    value: ColumnDeclaring<V>,
): CaseWhenExpression<T, V> {
    return CaseWhenExpression(
        caseExpr = this.caseValue,
        whenThenConditions = listOf(this.condition to value.asExpression()),
        whenSqlType = this.condition.sqlType,
        sqlType = value.sqlType,
    )
}

/**
 * DSL to build [CaseWhenExpression] from [CaseWhenExpression] with else branch.
 * [value] the value of else branch
 */
@Suppress("FunctionName")
public infix fun <T : Any, V : Any> CaseWhenExpression<T, V>.ELSE(value: ColumnDeclaring<V>): CaseWhenExpression<T, V> {
    return this.copy(elseExpr = value.asExpression())
}

/**
 * DSL to build [CaseWhenExpression] from [CaseWhenExpression] with else branch.
 *
 * [value] the value of else branch
 */
@Suppress("FunctionName")
public infix fun <T : Any, V : Any> CaseWhenExpression<T, V>.ELSE(value: V): CaseWhenExpression<T, V> {
    return this.copy(elseExpr = ArgumentExpression(value, this.sqlType))
}

/**
 * DSL to build [WhenExpression] from [CaseWhenExpression].
 *
 * [condition] the when condition
 */
@Suppress("FunctionName")
public infix fun <T : Any, V : Any> CaseWhenExpression<T, V>.WHEN(condition: ColumnDeclaring<T>): WhenExpression<T, V> {
    return WhenExpression(
        caseWhenExpression = this,
        condition = condition.asExpression(),
        caseValue = null
    )
}

/**
 * DSL to build [WhenExpression] from [CaseWhenExpression].
 *
 * [condition] the when condition
 */
@Suppress("FunctionName")
public infix fun <T : Any, V : Any> CaseWhenExpression<T, V>.WHEN(condition: T): WhenExpression<T, V> {
    return WhenExpression(
        caseWhenExpression = this,
        condition = ArgumentExpression(condition, this.whenSqlType),
        caseValue = null,
    )
}
