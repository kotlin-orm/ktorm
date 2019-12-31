/*
 * Copyright 2018-2020 the original author or authors.
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

package me.liuwj.ktorm.dsl

import me.liuwj.ktorm.expression.JoinExpression
import me.liuwj.ktorm.expression.JoinType
import me.liuwj.ktorm.expression.QuerySourceExpression
import me.liuwj.ktorm.schema.BaseTable
import me.liuwj.ktorm.schema.ColumnDeclaring

/**
 * Join the right table and return a [JoinExpression], translated to `cross join` in SQL.
 */
fun QuerySourceExpression.crossJoin(
    right: QuerySourceExpression,
    on: ColumnDeclaring<Boolean>? = null
): JoinExpression {
    return JoinExpression(type = JoinType.CROSS_JOIN, left = this, right = right, condition = on?.asExpression())
}

/**
 * Join the right table and return a [JoinExpression], translated to `cross join` in SQL.
 */
fun QuerySourceExpression.crossJoin(
    right: BaseTable<*>,
    on: ColumnDeclaring<Boolean>? = null
): JoinExpression {
    return crossJoin(right.asExpression(), on)
}

/**
 * Join the right table and return a [JoinExpression], translated to `cross join` in SQL.
 */
fun BaseTable<*>.crossJoin(
    right: QuerySourceExpression,
    on: ColumnDeclaring<Boolean>? = null
): JoinExpression {
    return asExpression().crossJoin(right, on)
}

/**
 * Join the right table and return a [JoinExpression], translated to `cross join` in SQL.
 */
fun BaseTable<*>.crossJoin(
    right: BaseTable<*>,
    on: ColumnDeclaring<Boolean>? = null
): JoinExpression {
    return crossJoin(right.asExpression(), on)
}

/**
 * Join the right table and return a [JoinExpression], translated to `inner join` in SQL.
 */
fun QuerySourceExpression.innerJoin(
    right: QuerySourceExpression,
    on: ColumnDeclaring<Boolean>? = null
): JoinExpression {
    return JoinExpression(type = JoinType.INNER_JOIN, left = this, right = right, condition = on?.asExpression())
}

/**
 * Join the right table and return a [JoinExpression], translated to `inner join` in SQL.
 */
fun QuerySourceExpression.innerJoin(
    right: BaseTable<*>,
    on: ColumnDeclaring<Boolean>? = null
): JoinExpression {
    return innerJoin(right.asExpression(), on)
}

/**
 * Join the right table and return a [JoinExpression], translated to `inner join` in SQL.
 */
fun BaseTable<*>.innerJoin(
    right: QuerySourceExpression,
    on: ColumnDeclaring<Boolean>? = null
): JoinExpression {
    return asExpression().innerJoin(right, on)
}

/**
 * Join the right table and return a [JoinExpression], translated to `inner join` in SQL.
 */
fun BaseTable<*>.innerJoin(
    right: BaseTable<*>,
    on: ColumnDeclaring<Boolean>? = null
): JoinExpression {
    return innerJoin(right.asExpression(), on)
}

/**
 * Join the right table and return a [JoinExpression], translated to `left join` in SQL.
 */
fun QuerySourceExpression.leftJoin(
    right: QuerySourceExpression,
    on: ColumnDeclaring<Boolean>? = null
): JoinExpression {
    return JoinExpression(type = JoinType.LEFT_JOIN, left = this, right = right, condition = on?.asExpression())
}

/**
 * Join the right table and return a [JoinExpression], translated to `left join` in SQL.
 */
fun QuerySourceExpression.leftJoin(
    right: BaseTable<*>,
    on: ColumnDeclaring<Boolean>? = null
): JoinExpression {
    return leftJoin(right.asExpression(), on)
}

/**
 * Join the right table and return a [JoinExpression], translated to `left join` in SQL.
 */
fun BaseTable<*>.leftJoin(
    right: QuerySourceExpression,
    on: ColumnDeclaring<Boolean>? = null
): JoinExpression {
    return asExpression().leftJoin(right, on)
}

/**
 * Join the right table and return a [JoinExpression], translated to `left join` in SQL.
 */
fun BaseTable<*>.leftJoin(
    right: BaseTable<*>,
    on: ColumnDeclaring<Boolean>? = null
): JoinExpression {
    return leftJoin(right.asExpression(), on)
}

/**
 * Join the right table and return a [JoinExpression], translated to `right join` in SQL.
 */
fun QuerySourceExpression.rightJoin(
    right: QuerySourceExpression,
    on: ColumnDeclaring<Boolean>? = null
): JoinExpression {
    return JoinExpression(type = JoinType.RIGHT_JOIN, left = this, right = right, condition = on?.asExpression())
}

/**
 * Join the right table and return a [JoinExpression], translated to `right join` in SQL.
 */
fun QuerySourceExpression.rightJoin(
    right: BaseTable<*>,
    on: ColumnDeclaring<Boolean>? = null
): JoinExpression {
    return rightJoin(right.asExpression(), on)
}

/**
 * Join the right table and return a [JoinExpression], translated to `right join` in SQL.
 */
fun BaseTable<*>.rightJoin(
    right: QuerySourceExpression,
    on: ColumnDeclaring<Boolean>? = null
): JoinExpression {
    return asExpression().rightJoin(right, on)
}

/**
 * Join the right table and return a [JoinExpression], translated to `right join` in SQL.
 */
fun BaseTable<*>.rightJoin(
    right: BaseTable<*>,
    on: ColumnDeclaring<Boolean>? = null
): JoinExpression {
    return rightJoin(right.asExpression(), on)
}
