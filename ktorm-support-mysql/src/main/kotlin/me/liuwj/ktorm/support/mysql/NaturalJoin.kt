/*
 * Copyright 2018-2019 the original author or authors.
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

@file:Suppress("MatchingDeclarationName")

package me.liuwj.ktorm.support.mysql

import me.liuwj.ktorm.expression.QuerySourceExpression
import me.liuwj.ktorm.schema.BaseTable

/**
 * MySQL natural join expression.
 *
 * @property left the left table.
 * @property right the right table.
 */
data class NaturalJoinExpression(
    val left: QuerySourceExpression,
    val right: QuerySourceExpression,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : QuerySourceExpression()

/**
 * Join the right table and return a [NaturalJoinExpression], translated to `natural join` in MySQL.
 */
fun QuerySourceExpression.naturalJoin(right: QuerySourceExpression): NaturalJoinExpression {
    return NaturalJoinExpression(left = this, right = right)
}

/**
 * Join the right table and return a [NaturalJoinExpression], translated to `natural join` in MySQL.
 */
fun QuerySourceExpression.naturalJoin(right: BaseTable<*>): NaturalJoinExpression {
    return naturalJoin(right.asExpression())
}

/**
 * Join the right table and return a [NaturalJoinExpression], translated to `natural join` in MySQL.
 */
fun BaseTable<*>.naturalJoin(right: QuerySourceExpression): NaturalJoinExpression {
    return asExpression().naturalJoin(right)
}

/**
 * Join the right table and return a [NaturalJoinExpression], translated to `natural join` in MySQL.
 */
fun BaseTable<*>.naturalJoin(right: BaseTable<*>): NaturalJoinExpression {
    return naturalJoin(right.asExpression())
}
