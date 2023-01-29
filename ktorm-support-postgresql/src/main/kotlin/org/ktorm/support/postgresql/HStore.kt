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
import org.ktorm.expression.ScalarExpression
import org.ktorm.schema.BooleanSqlType
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.SqlType
import org.ktorm.schema.VarcharSqlType

/**
 * Enum for `hstore` operators.
 */
public enum class HStoreExpressionType(private val value: String) {

    /**
     * HStore get-value operator, translated to the -> operator in PostgreSQL.
     */
    GET("->"),

    /**
     * HStore concatenate operator, translated to the || operator in PostgreSQL.
     */
    CONCATENATE("||"),

    /**
     * HStore contains-key operator, translated to the ? operator in PostgreSQL.
     */
    CONTAINS_KEY("?"),

    /**
     * HStore contains-all-keys operator, translated to the ?& operator in PostgreSQL.
     */
    CONTAINS_ALL("?&"),

    /**
     * HStore contains-any-keys operator, translated to the ?| operator in PostgreSQL.
     */
    CONTAINS_ANY("?|"),

    /**
     * HStore contains operator, translated to the @> operator in PostgreSQL.
     */
    CONTAINS("@>"),

    /**
     * HStore contained-in operator, translated to the <@ operator in PostgreSQL.
     */
    CONTAINED_IN("<@"),

    /**
     * HStore delete operator, translated to the - operator in PostgreSQL.
     */
    DELETE("-");

    override fun toString(): String {
        // Escape for JDBC prepared statements.
        return value.replace("?", "??")
    }
}

/**
 * Expression class represents PostgreSQL `hstore` operations.
 *
 * @property type the expression's type.
 * @property left the expression's left operand.
 * @property right the expression's right operand.
 */
public data class HStoreExpression<T : Any>(
    val type: HStoreExpressionType,
    val left: ScalarExpression<HStore>,
    val right: ScalarExpression<*>,
    override val sqlType: SqlType<T>,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : ScalarExpression<T>()

/**
 * HStore get-value-for-key operator, translated to the -> operator in PostgreSQL.
 */
@JvmName("getValue")
public operator fun ColumnDeclaring<HStore>.get(expr: ColumnDeclaring<String>): HStoreExpression<String> {
    return HStoreExpression(HStoreExpressionType.GET, asExpression(), expr.asExpression(), VarcharSqlType)
}

/**
 * HStore get-value-for-key operator, translated to the -> operator in PostgreSQL.
 */
@JvmName("getValue")
public operator fun ColumnDeclaring<HStore>.get(argument: String): HStoreExpression<String> {
    return this[ArgumentExpression(argument, VarcharSqlType)]
}

/**
 * HStore get-values-for-keys operator, translated to the -> operator in PostgreSQL.
 */
@JvmName("getValues")
public operator fun ColumnDeclaring<HStore>.get(expr: ColumnDeclaring<TextArray>): HStoreExpression<TextArray> {
    return HStoreExpression(HStoreExpressionType.GET, asExpression(), expr.asExpression(), TextArraySqlType)
}

/**
 * HStore get-values-for-keys operator, translated to the -> operator in PostgreSQL.
 */
@JvmName("getValues")
public operator fun ColumnDeclaring<HStore>.get(argument: TextArray): HStoreExpression<TextArray> {
    return this[ArgumentExpression(argument, TextArraySqlType)]
}

/**
 * HStore concatenate operator, translated to the || operator in PostgreSQL.
 */
public operator fun ColumnDeclaring<HStore>.plus(expr: ColumnDeclaring<HStore>): HStoreExpression<HStore> {
    return HStoreExpression(HStoreExpressionType.CONCATENATE, asExpression(), expr.asExpression(), HStoreSqlType)
}

/**
 * HStore concatenate operator, translated to the || operator in PostgreSQL.
 */
public operator fun ColumnDeclaring<HStore>.plus(argument: HStore): HStoreExpression<HStore> {
    return this + wrapArgument(argument)
}

/**
 * HStore contains-key operator, translated to the ? operator in PostgreSQL.
 */
public fun ColumnDeclaring<HStore>.containsKey(expr: ColumnDeclaring<String>): HStoreExpression<Boolean> {
    return HStoreExpression(HStoreExpressionType.CONTAINS_KEY, asExpression(), expr.asExpression(), BooleanSqlType)
}

/**
 * HStore contains-key operator, translated to the ? operator in PostgreSQL.
 */
public fun ColumnDeclaring<HStore>.containsKey(argument: String): HStoreExpression<Boolean> {
    return this.containsKey(ArgumentExpression(argument, VarcharSqlType))
}

/**
 * HStore contains-all-keys operator, translated to the ?& operator in PostgreSQL.
 */
public fun ColumnDeclaring<HStore>.containsAll(expr: ColumnDeclaring<TextArray>): HStoreExpression<Boolean> {
    return HStoreExpression(HStoreExpressionType.CONTAINS_ALL, asExpression(), expr.asExpression(), BooleanSqlType)
}

/**
 * HStore contains-all-keys operator, translated to the ?& operator in PostgreSQL.
 */
public fun ColumnDeclaring<HStore>.containsAll(argument: TextArray): HStoreExpression<Boolean> {
    return this.containsAll(ArgumentExpression(argument, TextArraySqlType))
}

/**
 * HStore contains-any-keys operator, translated to the ?| operator in PostgreSQL.
 */
public fun ColumnDeclaring<HStore>.containsAny(expr: ColumnDeclaring<TextArray>): HStoreExpression<Boolean> {
    return HStoreExpression(HStoreExpressionType.CONTAINS_ANY, asExpression(), expr.asExpression(), BooleanSqlType)
}

/**
 * HStore contains-any-keys operator, translated to the ?| operator in PostgreSQL.
 */
public fun ColumnDeclaring<HStore>.containsAny(argument: TextArray): HStoreExpression<Boolean> {
    return this.containsAny(ArgumentExpression(argument, TextArraySqlType))
}

/**
 * HStore contains operator, translated to the @> operator in PostgreSQL.
 */
public fun ColumnDeclaring<HStore>.contains(expr: ColumnDeclaring<HStore>): HStoreExpression<Boolean> {
    return HStoreExpression(HStoreExpressionType.CONTAINS, asExpression(), expr.asExpression(), BooleanSqlType)
}

/**
 * HStore contains operator, translated to the @> operator in PostgreSQL.
 */
public fun ColumnDeclaring<HStore>.contains(argument: HStore): HStoreExpression<Boolean> {
    return this.contains(wrapArgument(argument))
}

/**
 * HStore contained-in operator, translated to the <@ operator in PostgreSQL.
 */
public fun ColumnDeclaring<HStore>.containedIn(expr: ColumnDeclaring<HStore>): HStoreExpression<Boolean> {
    return HStoreExpression(HStoreExpressionType.CONTAINED_IN, asExpression(), expr.asExpression(), BooleanSqlType)
}

/**
 * HStore contained-in operator, translated to the <@ operator in PostgreSQL.
 */
public fun ColumnDeclaring<HStore>.containedIn(argument: HStore): HStoreExpression<Boolean> {
    return this.containedIn(wrapArgument(argument))
}

/**
 * HStore delete-key operator, translated to the - operator in PostgreSQL.
 */
@JvmName("minusKey")
public operator fun ColumnDeclaring<HStore>.minus(expr: ColumnDeclaring<String>): HStoreExpression<HStore> {
    return HStoreExpression(HStoreExpressionType.DELETE, asExpression(), expr.asExpression(), HStoreSqlType)
}

/**
 * HStore delete-key operator, translated to the - operator in PostgreSQL.
 */
@JvmName("minusKey")
public operator fun ColumnDeclaring<HStore>.minus(argument: String): HStoreExpression<HStore> {
    return this - ArgumentExpression(argument, VarcharSqlType)
}

/**
 * HStore delete-keys operator, translated to the - operator in PostgreSQL.
 */
@JvmName("minusKeys")
public operator fun ColumnDeclaring<HStore>.minus(expr: ColumnDeclaring<TextArray>): HStoreExpression<HStore> {
    return HStoreExpression(HStoreExpressionType.DELETE, asExpression(), expr.asExpression(), HStoreSqlType)
}

/**
 * HStore delete-keys operator, translated to the - operator in PostgreSQL.
 */
@JvmName("minusKeys")
public operator fun ColumnDeclaring<HStore>.minus(argument: TextArray): HStoreExpression<HStore> {
    return this - ArgumentExpression(argument, TextArraySqlType)
}

/**
 * HStore delete-matching-pairs operator, translated to the - operator in PostgreSQL.
 */
@JvmName("minusMatching")
public operator fun ColumnDeclaring<HStore>.minus(expr: ColumnDeclaring<HStore>): HStoreExpression<HStore> {
    return HStoreExpression(HStoreExpressionType.DELETE, asExpression(), expr.asExpression(), HStoreSqlType)
}

/**
 * HStore delete-matching-pairs operator, translated to the - operator in PostgreSQL.
 */
@JvmName("minusMatching")
public operator fun ColumnDeclaring<HStore>.minus(argument: HStore): HStoreExpression<HStore> {
    return this - wrapArgument(argument)
}
