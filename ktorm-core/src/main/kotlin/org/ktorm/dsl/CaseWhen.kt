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

@file:Suppress("FunctionName")

package org.ktorm.dsl

import org.ktorm.expression.CaseWhenExpression
import org.ktorm.schema.*

/**
 * Helper class used to build case-when SQL DSL. See [CaseWhenExpression].
 */
public data class CaseWhen<T : Any, R : Any>(
    val operand: ColumnDeclaring<T>?,
    val whenClauses: List<Pair<ColumnDeclaring<T>, ColumnDeclaring<R>>> = emptyList(),
    val elseClause: ColumnDeclaring<R>? = null
)

/**
 * Return type for [WHEN] function, call its extension function [THEN] to finish a SQL when clause.
 */
public data class WhenContinuation<T : Any, R : Any>(
    val parent: CaseWhen<T, R>,
    val condition: ColumnDeclaring<T>
)

/**
 * Starts a searched case-when DSL.
 */
public fun CASE(): CaseWhen<Boolean, Nothing> {
    return CaseWhen(operand = null)
}

/**
 * Starts a simple case-when DSL with the given [operand].
 */
public fun <T : Any> CASE(operand: ColumnDeclaring<T>): CaseWhen<T, Nothing> {
    return CaseWhen(operand)
}

/**
 * Starts a when clause with the given [condition].
 */
public fun <T : Any, R : Any> CaseWhen<T, R>.WHEN(condition: ColumnDeclaring<T>): WhenContinuation<T, R> {
    return WhenContinuation(this, condition)
}

/**
 * Finishes the current when clause with the given [result].
 */
@JvmName("firstTHEN")
@Suppress("UNCHECKED_CAST")
public fun <T : Any, R : Any> WhenContinuation<T, Nothing>.THEN(result: ColumnDeclaring<R>): CaseWhen<T, R> {
    return (this as WhenContinuation<T, R>).THEN(result)
}

/**
 * Finishes the current when clause with the given [result].
 */
public fun <T : Any, R : Any> WhenContinuation<T, R>.THEN(result: ColumnDeclaring<R>): CaseWhen<T, R> {
    return parent.copy(whenClauses = parent.whenClauses + Pair(condition, result))
}

/**
 * Specifies the else clause for the case-when DSL.
 */
public fun <T : Any, R : Any> CaseWhen<T, R>.ELSE(result: ColumnDeclaring<R>): CaseWhen<T, R> {
    return this.copy(elseClause = result)
}

/**
 * Finishes the case-when DSL and returns a [CaseWhenExpression].
 */
public fun <R : Any> CaseWhen<*, R>.END(): CaseWhenExpression<R> {
    if (whenClauses.isEmpty()) {
        throw IllegalStateException("A case-when DSL must have at least one when clause.")
    }

    return CaseWhenExpression(
        operand = operand?.asExpression(),
        whenClauses = whenClauses.map { (condition, result) -> Pair(condition.asExpression(), result.asExpression()) },
        elseClause = elseClause?.asExpression(),
        sqlType = whenClauses.map { (_, result) -> result.sqlType }.first()
    )
}
