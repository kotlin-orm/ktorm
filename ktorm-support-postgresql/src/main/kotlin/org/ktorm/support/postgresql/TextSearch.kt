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

package org.ktorm.support.postgresql

import org.ktorm.expression.ArgumentExpression
import org.ktorm.expression.FunctionExpression
import org.ktorm.expression.ScalarExpression
import org.ktorm.schema.BooleanSqlType
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.SqlType
import org.ktorm.schema.TextSqlType

/**
 * Enum for text search operators.
 * See: https://www.postgresql.org/docs/current/functions-textsearch.html
 */
public enum class TextSearchExpressionType(private val value: String) {

    /**
     * The text search match operator checks whether a tsquery matches a tsvector or a text.
     * Translated to the @@ operator in PostgreSQL.
     */
    MATCH("@@"),

    /**
     * The text search concatenate operator concatenates tsvectors.
     * Translated to the || operator in PostgreSQL.
     */
    CONCAT("||"),

    /**
     * The text search AND operator combines tsquerys by an AND.
     * Translated to the && operator in PostgreSQL.
     */
    AND("&&"),

    /**
     * The text search OR operator combines tsquerys by an OR.
     * Translated to the || operator in PostgreSQL.
     */
    OR("||"),

    /**
     * The text search NOT operator negates a tsquery.
     * Translated to the !! operator in PostgreSQL.
     */
    NOT("!!"),

    /**
     * The text search phrase operator creates a tsquery that checks whether 2 tsquery follow each other.
     * Translated to the <-> operator in PostgreSQL.
     */
    PHRASE("<->"),

    /**
     * The text search contains operator checks whether a tsquery contains another tsquery.
     * Translated to the @> operator in PostgreSQL.
     */
    CONTAINS("@>"),

    /**
     * The text search contained in operator checks whether a tsquery is contained in another tsquery.
     * Translated to the <@ operator in PostgreSQL.
     */
    CONTAINED_IN("<@");

    override fun toString(): String = value
}

/**
 * Expression class that represents PostgreSQL text search operations.
 *
 * @property type the expression's type.
 * @property left the expression's left operand.
 * @property right the expression's right operand.
 */
public data class TextSearchExpression<T : Any>(
    val type: TextSearchExpressionType,
    val left: ScalarExpression<*>?,
    val right: ScalarExpression<*>,
    override val sqlType: SqlType<T>,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : ScalarExpression<T>()

/**
 * Checks whether a tsvector matches a tsquery. Translated to the @@ operator in PostgreSQL.
 */
@JvmName("matchTSQuery")
public infix fun ColumnDeclaring<TSVector>.match(expr: ColumnDeclaring<TSQuery>): TextSearchExpression<Boolean> =
    TextSearchExpression(TextSearchExpressionType.MATCH, asExpression(), expr.asExpression(), BooleanSqlType)

/**
 * Checks whether a tsquery matches a tsvector. Translated to the @@ operator in PostgreSQL.
 */
@JvmName("matchTSVector")
public infix fun ColumnDeclaring<TSQuery>.match(expr: ColumnDeclaring<TSVector>): TextSearchExpression<Boolean> =
    TextSearchExpression(TextSearchExpressionType.MATCH, asExpression(), expr.asExpression(), BooleanSqlType)

/**
 * Checks whether a text matches a tsquery. Translated to the @@ operator in PostgreSQL.
 */
@JvmName("matchesTSQuery")
public infix fun ColumnDeclaring<String>.match(expr: ColumnDeclaring<TSQuery>): TextSearchExpression<Boolean> =
    TextSearchExpression(TextSearchExpressionType.MATCH, asExpression(), expr.asExpression(), BooleanSqlType)

/**
 * Concatenates tsvectors. Translated to the || operator in PostgreSQL.
 */
public infix fun ColumnDeclaring<TSVector>.concat(expr: ColumnDeclaring<TSVector>): TextSearchExpression<TSVector> =
    TextSearchExpression(TextSearchExpressionType.CONCAT, asExpression(), expr.asExpression(), TSVectorSqlType)

/**
 * Combines tsquerys by an AND. Translated to the && operator in PostgreSQL.
 */
public infix fun ColumnDeclaring<TSQuery>.and(expr: ColumnDeclaring<TSQuery>): TextSearchExpression<TSQuery> =
    TextSearchExpression(TextSearchExpressionType.AND, asExpression(), expr.asExpression(), TSQuerySqlType)

/**
 * Combines tsquerys by an OR. Translated to the || operator in PostgreSQL.
 */
public infix fun ColumnDeclaring<TSQuery>.or(expr: ColumnDeclaring<TSQuery>): TextSearchExpression<TSQuery> =
    TextSearchExpression(TextSearchExpressionType.OR, asExpression(), expr.asExpression(), TSQuerySqlType)

/**
 * Negates a tsquery. Translated to the !! operator in PostgreSQL.
 */
public fun not(expr: ColumnDeclaring<TSQuery>): TextSearchExpression<TSQuery> =
    TextSearchExpression(TextSearchExpressionType.NOT, null, expr.asExpression(), TSQuerySqlType)

/**
 * Creates a tsquery that checks whether 2 tsquery follow each other. Translated to the <-> operator in PostgreSQL.
 */
public infix fun ColumnDeclaring<TSQuery>.followedBy(expr: ColumnDeclaring<TSQuery>): TextSearchExpression<TSQuery> =
    TextSearchExpression(TextSearchExpressionType.PHRASE, asExpression(), expr.asExpression(), TSQuerySqlType)

/**
 * Checks whether a tsquery contains another tsquery. Translated to the @> operator in PostgreSQL.
 */
public infix fun ColumnDeclaring<TSQuery>.contains(expr: ColumnDeclaring<TSQuery>): TextSearchExpression<Boolean> =
    TextSearchExpression(TextSearchExpressionType.CONTAINS, asExpression(), expr.asExpression(), BooleanSqlType)

/**
 * Checks whether a tsquery is contained in another tsquery. Translated to the <@ operator in PostgreSQL.
 */
public infix fun ColumnDeclaring<TSQuery>.containedIn(expr: ColumnDeclaring<TSQuery>): TextSearchExpression<Boolean> =
    TextSearchExpression(TextSearchExpressionType.CONTAINED_IN, asExpression(), expr.asExpression(), BooleanSqlType)

/**
 * Calls the to_tsquery function of Postgres, which converts a text to a tsquery.
 * See: https://www.postgresql.org/docs/current/functions-textsearch.html
 *
 * @param config The configuration of the query. E.g.: "english" or "simple"
 * @param value The text that should be converted to a tsquery
 */
public fun toTSQuery(config: RegConfig, value: String): FunctionExpression<TSQuery> = FunctionExpression(
    functionName = "to_tsquery",
    arguments = listOf(ArgumentExpression(config, RegConfigSqlType), ArgumentExpression(value, TextSqlType)),
    sqlType = TSQuerySqlType
)

/**
 * Calls the to_tsvector function of Postgres, which converts a text to a tsvector.
 * See: https://www.postgresql.org/docs/current/functions-textsearch.html
 *
 * @param config The configuration of the query. E.g.: "english" or "simple"
 * @param value The text that should be converted to a tsvector
 */
public fun toTSVector(config: RegConfig, value: String): FunctionExpression<TSVector> = FunctionExpression(
    functionName = "to_tsvector",
    arguments = listOf(ArgumentExpression(config, RegConfigSqlType), ArgumentExpression(value, TextSqlType)),
    sqlType = TSVectorSqlType
)
