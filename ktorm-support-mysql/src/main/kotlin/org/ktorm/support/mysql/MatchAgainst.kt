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

import org.ktorm.expression.ColumnExpression
import org.ktorm.expression.ScalarExpression
import org.ktorm.schema.BooleanSqlType
import org.ktorm.schema.Column
import org.ktorm.schema.SqlType

/**
 * Enum class represents search modifiers in MySQL `match ... against ...` expressions.
 * See https://dev.mysql.com/doc/refman/5.5/en/fulltext-search.html
 */
public enum class SearchModifier(private val value: String) {

    /**
     * Search modifier `in natural language mode`.
     */
    IN_NATURAL_LANGUAGE_MODE("in natural language mode"),

    /**
     * Search modifier `in natural language mode with query expansion`.
     */
    IN_NATURAL_LANGUAGE_MODE_WITH_QUERY_EXPANSION("in natural language mode with query expansion"),

    /**
     * Search modifier `in boolean mode`.
     */
    IN_BOOLEAN_MODE("in boolean mode"),

    /**
     * Search modifier `with query expansion`.
     */
    WITH_QUERY_EXPANSION("with query expansion");

    override fun toString(): String {
        return value
    }
}

/**
 * Match against expression, represents an `match ... against ...` operation in MySQL.
 * See https://dev.mysql.com/doc/refman/5.5/en/insert-on-duplicate.html
 *
 * @property matchColumns columns to be searched.
 * @property searchString the search string.
 * @property searchModifier optional modifier that indicates what type of search to perform.
 */
public data class MatchAgainstExpression(
    val matchColumns: List<ColumnExpression<*>>,
    val searchString: String,
    val searchModifier: SearchModifier? = null,
    override val sqlType: SqlType<Boolean> = BooleanSqlType,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : ScalarExpression<Boolean>()

/**
 * Intermediate class that wraps the search columns of a [MatchAgainstExpression].
 */
public class MatchColumns(columns: List<ColumnExpression<*>>) : List<ColumnExpression<*>> by columns

/**
 * Return an intermediate object that wraps the columns to be searched. We can continue to call [against] on the
 * returned object to create a [MatchAgainstExpression] that searches the wrapped columns.
 */
public fun match(vararg columns: Column<*>): MatchColumns {
    return MatchColumns(columns.map { it.asExpression() })
}

/**
 * Create a [MatchAgainstExpression] that searches on the current [MatchColumns].
 * Translated to `match (col1, col2) against (searchString modifier)` in SQL.
 */
public fun MatchColumns.against(searchString: String, modifier: SearchModifier? = null): MatchAgainstExpression {
    return MatchAgainstExpression(this, searchString, modifier)
}
