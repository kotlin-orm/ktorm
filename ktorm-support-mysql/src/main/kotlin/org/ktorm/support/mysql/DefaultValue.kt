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

@file:Suppress("MatchingDeclarationName")

package org.ktorm.support.mysql

import org.ktorm.expression.ScalarExpression
import org.ktorm.schema.Column
import org.ktorm.schema.SqlType
import java.util.Collections.emptyMap

/**
 * Default value expression, translated to the `default` keyword in MySQL, used in insert statements.
 *
 * For example:
 *
 * ```sql
 * insert into table (column1, column2) values (default, ?)
 * ```
 */
public data class DefaultValueExpression<T : Any>(
    override val sqlType: SqlType<T>,
    override val isLeafNode: Boolean = true,
    override val extraProperties: Map<String, Any> = emptyMap()
) : ScalarExpression<T>()

/**
 * Return a default value for [this] column, see [DefaultValueExpression].
 */
public fun <T : Any> Column<T>.defaultValue(): DefaultValueExpression<T> = DefaultValueExpression(this.sqlType)
