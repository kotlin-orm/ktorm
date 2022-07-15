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
import java.util.*

public fun <T : Any> case(
    resultType: SqlType<T>,
    builder: CaseWhenBuilder<Boolean, T>.() -> Unit,
): CaseWhenExpression<Boolean, T> {

    val caseWhenBuilder = CaseWhenBuilder(BooleanSqlType, resultType)
    builder(caseWhenBuilder)

    return CaseWhenExpression(
        whenSqlType = BooleanSqlType,
        whenThenConditions = caseWhenBuilder.whenThenExprList,
        elseExpr = caseWhenBuilder.elseExpr,
        sqlType = resultType
    )
}

public fun <T : Any, V : Any> case(
    resultType: SqlType<V>,
    value: ColumnDeclaring<T>,
    builder: CaseWhenBuilder<T, V>.() -> Unit,
): CaseWhenExpression<T, V> {


    val caseWhenBuilder = CaseWhenBuilder(value.sqlType, resultType)
    builder(caseWhenBuilder)

    return CaseWhenExpression(
        whenSqlType = value.sqlType,
        caseExpr = value.asExpression(),
        whenThenConditions = caseWhenBuilder.whenThenExprList,
        elseExpr = caseWhenBuilder.elseExpr,
        sqlType = resultType
    )
}


public class CaseWhenBuilder<T : Any, V : Any>(
    private val caseSqlType: SqlType<T>,
    private val resultType: SqlType<V>,
) {

    internal val whenThenExprList: LinkedList<Pair<ScalarExpression<T>, ScalarExpression<V>>> = LinkedList()
    internal var elseExpr: ScalarExpression<V>? = null

    public fun whenThen(`when`: T, then: ScalarExpression<V>) {
        whenThenExprList.add(
            ArgumentExpression(`when`, caseSqlType) to then
        )
    }

    public fun whenThen(`when`: T, then: V?) {
        whenThenExprList.add(
            ArgumentExpression(`when`, caseSqlType) to ArgumentExpression(then, resultType)
        )
    }

    public fun whenThen(`when`: ScalarExpression<T>, then: ScalarExpression<V>) {
        whenThenExprList.add(`when` to then)
    }

    public fun whenThen(`when`: ScalarExpression<T>, then: V?) {
        whenThenExprList.add(`when` to ArgumentExpression(then, resultType))
    }

    public fun `else`(`else`: ScalarExpression<V>) {
        this.elseExpr = `else`
    }

    public fun `else`(`else`: V?) {
        this.elseExpr = ArgumentExpression(`else`, resultType)
    }


}

public data class CaseExpression<T : Any> internal constructor(
    internal val caseValue: ScalarExpression<T>? = null,
    internal val caseSqlType: SqlType<T>,
)

@Suppress("FunctionName")
public fun CASE(): CaseExpression<Boolean> {
    return CaseExpression(null, BooleanSqlType)
}

@Suppress("FunctionName")
public fun <T : Any> CASE(caseValue: ColumnDeclaring<T>): CaseExpression<T> {
    return CaseExpression(caseValue.asExpression(), caseValue.sqlType)
}

public data class WhenExpression<T : Any> internal constructor(
    internal val caseValue: ScalarExpression<T>?,
    internal val condition: ScalarExpression<T>,
)

@Suppress("FunctionName")
public infix fun <T : Any> CaseExpression<T>.WHEN(condition: ColumnDeclaring<T>): WhenExpression<T> {
    return WhenExpression(
        this.caseValue,
        condition.asExpression(),
    )
}

@Suppress("FunctionName")
public infix fun <T : Any> CaseExpression<T>.WHEN(condition: T): WhenExpression<T> {
    return WhenExpression(
        this.caseValue,
        ArgumentExpression(condition, this.caseSqlType),
    )
}


@Suppress("FunctionName")
public infix fun <T : Any, V : Any> WhenExpression<T>.THEN(value: ColumnDeclaring<V>): CaseWhenExpression<T, V> {
    return CaseWhenExpression(
        caseExpr = this.caseValue,
        whenThenConditions = listOf(this.condition to value.asExpression()),
        whenSqlType = this.condition.sqlType,
        sqlType = value.sqlType,
    )
}

@Suppress("FunctionName")
public infix fun <T : Any, V : Any> CaseWhenExpression<T, V>.ELSE(value: ColumnDeclaring<V>): CaseWhenExpression<T, V> {
    return this.copy(elseExpr = value.asExpression())
}

@Suppress("FunctionName")
public infix fun <T : Any, V : Any> CaseWhenExpression<T, V>.ELSE(value: V): CaseWhenExpression<T, V> {
    return this.copy(elseExpr = ArgumentExpression(value, this.sqlType))
}

public data class When2Expression<T : Any, V : Any> internal constructor(
    internal val caseWhenExpression: CaseWhenExpression<T, V>,
    internal val condition: ScalarExpression<T>,
)

@Suppress("FunctionName")
public infix fun <T : Any, V : Any> CaseWhenExpression<T, V>.WHEN(condition: ColumnDeclaring<T>): When2Expression<T, V> {
    return When2Expression(
        caseWhenExpression = this,
        condition = condition.asExpression(),
    )
}

@Suppress("FunctionName")
public infix fun <T : Any, V : Any> CaseWhenExpression<T, V>.WHEN(condition: T): When2Expression<T, V> {
    return When2Expression(
        caseWhenExpression = this,
        condition = ArgumentExpression(condition, this.whenSqlType),
    )
}

@Suppress("FunctionName")
public infix fun <T : Any, V : Any> When2Expression<T, V>.THEN(value: ColumnDeclaring<V>): CaseWhenExpression<T, V> {
    return this.caseWhenExpression.copy(
        whenThenConditions = this.caseWhenExpression.whenThenConditions + (this.condition to value.asExpression())
    )
}

@Suppress("FunctionName")
public infix fun <T : Any, V : Any> When2Expression<T, V>.THEN(value: V): CaseWhenExpression<T, V> {
    return this.caseWhenExpression.copy(
        whenThenConditions = this.caseWhenExpression.whenThenConditions + (this.condition to ArgumentExpression(
            value,
            this.caseWhenExpression.sqlType
        ))
    )
}


