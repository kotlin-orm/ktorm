/*
 * Copyright 2018-2024 the original author or authors.
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

package org.ktorm.support.mysql

import org.ktorm.dsl.QuerySource
import org.ktorm.expression.QuerySourceExpression
import org.ktorm.schema.BaseTable

/**
 * MySQL natural join expression.
 *
 * @property left the left table.
 * @property right the right table.
 */
public data class NaturalJoinExpression(
    val left: QuerySourceExpression,
    val right: QuerySourceExpression,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : QuerySourceExpression()

/**
 * Join the right table and return a new [QuerySource], translated to `natural join` in SQL.
 *
 * @since 2.7
 */
public fun QuerySource.naturalJoin(right: BaseTable<*>): QuerySource {
    return this.copy(expression = NaturalJoinExpression(left = expression, right = right.asExpression()))
}
