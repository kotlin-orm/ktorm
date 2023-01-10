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

package org.ktorm.dsl

import org.ktorm.expression.*
import org.ktorm.schema.*

// ---- Unary operators... ----

/**
 * Check if the current column or expression is null, translated to `is null` in SQL.
 */
public fun ColumnDeclaring<*>.isNull(): UnaryExpression<Boolean> {
    return UnaryExpression(UnaryExpressionType.IS_NULL, asExpression(), BooleanSqlType)
}

/**
 * Check if the current column or expression is not null, translated to `is not null` in SQL.
 */
public fun ColumnDeclaring<*>.isNotNull(): UnaryExpression<Boolean> {
    return UnaryExpression(UnaryExpressionType.IS_NOT_NULL, asExpression(), BooleanSqlType)
}

/**
 * Unary minus operator, translated to `-` in SQL.
 */
public operator fun <T : Number> ColumnDeclaring<T>.unaryMinus(): UnaryExpression<T> {
    return UnaryExpression(UnaryExpressionType.UNARY_MINUS, asExpression(), sqlType)
}

/**
 * Unary plus operator, translated to `+` in SQL.
 */
public operator fun <T : Number> ColumnDeclaring<T>.unaryPlus(): UnaryExpression<T> {
    return UnaryExpression(UnaryExpressionType.UNARY_PLUS, asExpression(), sqlType)
}

/**
 * Negative operator, translated to the `not` keyword in SQL.
 */
public operator fun ColumnDeclaring<Boolean>.not(): UnaryExpression<Boolean> {
    return UnaryExpression(UnaryExpressionType.NOT, asExpression(), BooleanSqlType)
}

// ---- Plus(+) ----

/**
 * Plus operator, translated to `+` in SQL.
 */
public infix operator fun <T : Number> ColumnDeclaring<T>.plus(expr: ColumnDeclaring<T>): BinaryExpression<T> {
    return BinaryExpression(BinaryExpressionType.PLUS, asExpression(), expr.asExpression(), sqlType)
}

/**
 * Plus operator, translated to `+` in SQL.
 */
public infix operator fun <T : Number> ColumnDeclaring<T>.plus(value: T): BinaryExpression<T> {
    return this + wrapArgument(value)
}

/**
 * Plus operator, translated to `+` in SQL.
 */
public infix operator fun <T : Number> T.plus(expr: ColumnDeclaring<T>): BinaryExpression<T> {
    return expr.wrapArgument(this) + expr
}

// ------- Minus(-) -----------

/**
 * Minus operator, translated to `-` in SQL.
 */
public infix operator fun <T : Number> ColumnDeclaring<T>.minus(expr: ColumnDeclaring<T>): BinaryExpression<T> {
    return BinaryExpression(BinaryExpressionType.MINUS, asExpression(), expr.asExpression(), sqlType)
}

/**
 * Minus operator, translated to `-` in SQL.
 */
public infix operator fun <T : Number> ColumnDeclaring<T>.minus(value: T): BinaryExpression<T> {
    return this - wrapArgument(value)
}

/**
 * Minus operator, translated to `-` in SQL.
 */
public infix operator fun <T : Number> T.minus(expr: ColumnDeclaring<T>): BinaryExpression<T> {
    return expr.wrapArgument(this) - expr
}

// -------- Times(*) -----------

/**
 * Multiply operator, translated to `*` in SQL.
 */
public infix operator fun <T : Number> ColumnDeclaring<T>.times(expr: ColumnDeclaring<T>): BinaryExpression<T> {
    return BinaryExpression(BinaryExpressionType.TIMES, asExpression(), expr.asExpression(), sqlType)
}

/**
 * Multiply operator, translated to `*` in SQL.
 */
public infix operator fun <T : Number> ColumnDeclaring<T>.times(value: T): BinaryExpression<T> {
    return this * wrapArgument(value)
}

/**
 * Multiply operator, translated to `*` in SQL.
 */
public infix operator fun <T : Number> T.times(expr: ColumnDeclaring<T>): BinaryExpression<T> {
    return expr.wrapArgument(this) * expr
}

// -------- Div(/) ----------

/**
 * Divide operator, translated to `/` in SQL.
 */
public infix operator fun <T : Number> ColumnDeclaring<T>.div(expr: ColumnDeclaring<T>): BinaryExpression<T> {
    return BinaryExpression(BinaryExpressionType.DIV, asExpression(), expr.asExpression(), sqlType)
}

/**
 * Divide operator, translated to `/` in SQL.
 */
public infix operator fun <T : Number> ColumnDeclaring<T>.div(value: T): BinaryExpression<T> {
    return this / wrapArgument(value)
}

/**
 * Divide operator, translated to `/` in SQL.
 */
public infix operator fun <T : Number> T.div(expr: ColumnDeclaring<T>): BinaryExpression<T> {
    return expr.wrapArgument(this) / expr
}

// -------- Rem(%) ----------

/**
 * Mod operator, translated to `%` in SQL.
 */
public infix operator fun <T : Number> ColumnDeclaring<T>.rem(expr: ColumnDeclaring<T>): BinaryExpression<T> {
    return BinaryExpression(BinaryExpressionType.REM, asExpression(), expr.asExpression(), sqlType)
}

/**
 * Mod operator, translated to `%` in SQL.
 */
public infix operator fun <T : Number> ColumnDeclaring<T>.rem(value: T): BinaryExpression<T> {
    return this % wrapArgument(value)
}

/**
 * Mod operator, translated to `%` in SQL.
 */
public infix operator fun <T : Number> T.rem(expr: ColumnDeclaring<T>): BinaryExpression<T> {
    return expr.wrapArgument(this) % expr
}

// -------- Like ----------

/**
 * Like operator, translated to the `like` keyword in SQL.
 */
public infix fun ColumnDeclaring<*>.like(expr: ColumnDeclaring<String>): BinaryExpression<Boolean> {
    return BinaryExpression(BinaryExpressionType.LIKE, asExpression(), expr.asExpression(), BooleanSqlType)
}

/**
 * Like operator, translated to the `like` keyword in SQL.
 */
public infix fun ColumnDeclaring<*>.like(value: String): BinaryExpression<Boolean> {
    return this like ArgumentExpression(value, VarcharSqlType)
}

/**
 * Not like operator, translated to the `not like` keyword in SQL.
 */
public infix fun ColumnDeclaring<*>.notLike(expr: ColumnDeclaring<String>): BinaryExpression<Boolean> {
    return BinaryExpression(BinaryExpressionType.NOT_LIKE, asExpression(), expr.asExpression(), BooleanSqlType)
}

/**
 * Not like operator, translated to the `not like` keyword in SQL.
 */
public infix fun ColumnDeclaring<*>.notLike(value: String): BinaryExpression<Boolean> {
    return this notLike ArgumentExpression(value, VarcharSqlType)
}

// --------- And ------------

/**
 * And operator, translated to the `and` keyword in SQL.
 */
public infix fun ColumnDeclaring<Boolean>.and(expr: ColumnDeclaring<Boolean>): BinaryExpression<Boolean> {
    return BinaryExpression(BinaryExpressionType.AND, asExpression(), expr.asExpression(), BooleanSqlType)
}

/**
 * And operator, translated to the `and` keyword in SQL.
 */
public infix fun ColumnDeclaring<Boolean>.and(value: Boolean): BinaryExpression<Boolean> {
    return this and wrapArgument(value)
}

/**
 * And operator, translated to the `and` keyword in SQL.
 */
public infix fun Boolean.and(expr: ColumnDeclaring<Boolean>): BinaryExpression<Boolean> {
    return expr.wrapArgument(this) and expr
}

// --------- Or ----------

/**
 * Or operator, translated to the `or` keyword in SQL.
 */
public infix fun ColumnDeclaring<Boolean>.or(expr: ColumnDeclaring<Boolean>): BinaryExpression<Boolean> {
    return BinaryExpression(BinaryExpressionType.OR, asExpression(), expr.asExpression(), BooleanSqlType)
}

/**
 * Or operator, translated to the `or` keyword in SQL.
 */
public infix fun ColumnDeclaring<Boolean>.or(value: Boolean): BinaryExpression<Boolean> {
    return this or wrapArgument(value)
}

/**
 * Or operator, translated to the `or` keyword in SQL.
 */
public infix fun Boolean.or(expr: ColumnDeclaring<Boolean>): BinaryExpression<Boolean> {
    return expr.wrapArgument(this) or expr
}

// -------- Xor ---------

/**
 * Xor operator, translated to the `xor` keyword in SQL.
 */
public infix fun ColumnDeclaring<Boolean>.xor(expr: ColumnDeclaring<Boolean>): BinaryExpression<Boolean> {
    return BinaryExpression(BinaryExpressionType.XOR, asExpression(), expr.asExpression(), BooleanSqlType)
}

/**
 * Xor operator, translated to the `xor` keyword in SQL.
 */
public infix fun ColumnDeclaring<Boolean>.xor(value: Boolean): BinaryExpression<Boolean> {
    return this xor wrapArgument(value)
}

/**
 * Xor operator, translated to the `xor` keyword in SQL.
 */
public infix fun Boolean.xor(expr: ColumnDeclaring<Boolean>): BinaryExpression<Boolean> {
    return expr.wrapArgument(this) xor expr
}

// ------- Less --------

/**
 * Less operator, translated to `<` in SQL.
 */
public infix fun <T : Comparable<T>> ColumnDeclaring<T>.less(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return BinaryExpression(BinaryExpressionType.LESS_THAN, asExpression(), expr.asExpression(), BooleanSqlType)
}

/**
 * Less operator, translated to `<` in SQL.
 */
public infix fun <T : Comparable<T>> ColumnDeclaring<T>.less(value: T): BinaryExpression<Boolean> {
    return this less wrapArgument(value)
}

/**
 * Less operator, translated to `<` in SQL.
 */
public infix fun <T : Comparable<T>> T.less(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return expr.wrapArgument(this) less expr
}

/**
 * Less operator, translated to `<` in SQL.
 *
 * @since 3.5.0
 */
public infix fun <T : Comparable<T>> ColumnDeclaring<T>.lt(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return BinaryExpression(BinaryExpressionType.LESS_THAN, asExpression(), expr.asExpression(), BooleanSqlType)
}

/**
 * Less operator, translated to `<` in SQL.
 *
 * @since 3.5.0
 */
public infix fun <T : Comparable<T>> ColumnDeclaring<T>.lt(value: T): BinaryExpression<Boolean> {
    return this lt wrapArgument(value)
}

/**
 * Less operator, translated to `<` in SQL.
 *
 * @since 3.5.0
 */
public infix fun <T : Comparable<T>> T.lt(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return expr.wrapArgument(this) lt expr
}

// ------- LessEq ---------

/**
 * Less-eq operator, translated to `<=` in SQL.
 */
public infix fun <T : Comparable<T>> ColumnDeclaring<T>.lessEq(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return BinaryExpression(
        type = BinaryExpressionType.LESS_THAN_OR_EQUAL,
        left = asExpression(),
        right = expr.asExpression(),
        sqlType = BooleanSqlType
    )
}

/**
 * Less-eq operator, translated to `<=` in SQL.
 */
public infix fun <T : Comparable<T>> ColumnDeclaring<T>.lessEq(value: T): BinaryExpression<Boolean> {
    return this lessEq wrapArgument(value)
}

/**
 * Less-eq operator, translated to `<=` in SQL.
 */
public infix fun <T : Comparable<T>> T.lessEq(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return expr.wrapArgument(this) lessEq expr
}

/**
 * Less-eq operator, translated to `<=` in SQL.
 *
 * @since 3.5.0
 */
public infix fun <T : Comparable<T>> ColumnDeclaring<T>.lte(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return BinaryExpression(
        type = BinaryExpressionType.LESS_THAN_OR_EQUAL,
        left = asExpression(),
        right = expr.asExpression(),
        sqlType = BooleanSqlType
    )
}

/**
 * Less-eq operator, translated to `<=` in SQL.
 *
 * @since 3.5.0
 */
public infix fun <T : Comparable<T>> ColumnDeclaring<T>.lte(value: T): BinaryExpression<Boolean> {
    return this lte wrapArgument(value)
}

/**
 * Less-eq operator, translated to `<=` in SQL.
 *
 * @since 3.5.0
 */
public infix fun <T : Comparable<T>> T.lte(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return expr.wrapArgument(this) lte expr
}

// ------- Greater ---------

/**
 * Greater operator, translated to `>` in SQL.
 */
public infix fun <T : Comparable<T>> ColumnDeclaring<T>.greater(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return BinaryExpression(BinaryExpressionType.GREATER_THAN, asExpression(), expr.asExpression(), BooleanSqlType)
}

/**
 * Greater operator, translated to `>` in SQL.
 */
public infix fun <T : Comparable<T>> ColumnDeclaring<T>.greater(value: T): BinaryExpression<Boolean> {
    return this greater wrapArgument(value)
}

/**
 * Greater operator, translated to `>` in SQL.
 */
public infix fun <T : Comparable<T>> T.greater(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return expr.wrapArgument(this) greater expr
}

/**
 * Greater operator, translated to `>` in SQL.
 *
 * @since 3.5.0
 */
public infix fun <T : Comparable<T>> ColumnDeclaring<T>.gt(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return BinaryExpression(BinaryExpressionType.GREATER_THAN, asExpression(), expr.asExpression(), BooleanSqlType)
}

/**
 * Greater operator, translated to `>` in SQL.
 *
 * @since 3.5.0
 */
public infix fun <T : Comparable<T>> ColumnDeclaring<T>.gt(value: T): BinaryExpression<Boolean> {
    return this gt wrapArgument(value)
}

/**
 * Greater operator, translated to `>` in SQL.
 *
 * @since 3.5.0
 */
public infix fun <T : Comparable<T>> T.gt(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return expr.wrapArgument(this) gt expr
}

// -------- GreaterEq ---------

/**
 * Greater-eq operator, translated to `>=` in SQL.
 */
public infix fun <T : Comparable<T>> ColumnDeclaring<T>.greaterEq(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return BinaryExpression(
        type = BinaryExpressionType.GREATER_THAN_OR_EQUAL,
        left = asExpression(),
        right = expr.asExpression(),
        sqlType = BooleanSqlType
    )
}

/**
 * Greater-eq operator, translated to `>=` in SQL.
 */
public infix fun <T : Comparable<T>> ColumnDeclaring<T>.greaterEq(value: T): BinaryExpression<Boolean> {
    return this greaterEq wrapArgument(value)
}

/**
 * Greater-eq operator, translated to `>=` in SQL.
 */
public infix fun <T : Comparable<T>> T.greaterEq(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return expr.wrapArgument(this) greaterEq expr
}

/**
 * Greater-eq operator, translated to `>=` in SQL.
 *
 * @since 3.5.0
 */
public infix fun <T : Comparable<T>> ColumnDeclaring<T>.gte(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return BinaryExpression(
        type = BinaryExpressionType.GREATER_THAN_OR_EQUAL,
        left = asExpression(),
        right = expr.asExpression(),
        sqlType = BooleanSqlType
    )
}

/**
 * Greater-eq operator, translated to `>=` in SQL.
 *
 * @since 3.5.0
 */
public infix fun <T : Comparable<T>> ColumnDeclaring<T>.gte(value: T): BinaryExpression<Boolean> {
    return this gte wrapArgument(value)
}

/**
 * Greater-eq operator, translated to `>=` in SQL.
 *
 * @since 3.5.0
 */
public infix fun <T : Comparable<T>> T.gte(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return expr.wrapArgument(this) gte expr
}

// -------- Eq ---------

/**
 * Equal operator, translated to `=` in SQL.
 */
public infix fun <T : Any> ColumnDeclaring<T>.eq(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return BinaryExpression(BinaryExpressionType.EQUAL, asExpression(), expr.asExpression(), BooleanSqlType)
}

/**
 * Equal operator, translated to `=` in SQL.
 */
public infix fun <T : Any> ColumnDeclaring<T>.eq(value: T): BinaryExpression<Boolean> {
    return this eq wrapArgument(value)
}

/**
 * Equal operator, translated to `=` in SQL.
 */
public infix fun <T : Any> T.eq(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return expr.wrapArgument(this) eq expr
}

// ------- NotEq -------

/**
 * Not-equal operator, translated to `<>` in SQL.
 */
public infix fun <T : Any> ColumnDeclaring<T>.notEq(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return BinaryExpression(BinaryExpressionType.NOT_EQUAL, asExpression(), expr.asExpression(), BooleanSqlType)
}

/**
 * Not-equal operator, translated to `<>` in SQL.
 */
public infix fun <T : Any> ColumnDeclaring<T>.notEq(value: T): BinaryExpression<Boolean> {
    return this notEq wrapArgument(value)
}

/**
 * Not-equal operator, translated to `<>` in SQL.
 */
public infix fun <T : Any> T.notEq(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return expr.wrapArgument(this) notEq expr
}

/**
 * Not-equal operator, translated to `<>` in SQL.
 *
 * @since 3.5.0
 */
public infix fun <T : Any> ColumnDeclaring<T>.neq(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return BinaryExpression(BinaryExpressionType.NOT_EQUAL, asExpression(), expr.asExpression(), BooleanSqlType)
}

/**
 * Not-equal operator, translated to `<>` in SQL.
 *
 * @since 3.5.0
 */
public infix fun <T : Any> ColumnDeclaring<T>.neq(value: T): BinaryExpression<Boolean> {
    return this neq wrapArgument(value)
}

/**
 * Not-equal operator, translated to `<>` in SQL.
 *
 * @since 3.5.0
 */
public infix fun <T : Any> T.neq(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return expr.wrapArgument(this) neq expr
}

// ---- Between ----

/**
 * Between operator, translated to `between .. and ..` in SQL.
 */
public infix fun <T : Comparable<T>> ColumnDeclaring<T>.between(range: ClosedRange<T>): BetweenExpression {
    return BetweenExpression(asExpression(), wrapArgument(range.start), wrapArgument(range.endInclusive))
}

/**
 * Not-between operator, translated to `not between .. and ..` in SQL.
 */
public infix fun <T : Comparable<T>> ColumnDeclaring<T>.notBetween(range: ClosedRange<T>): BetweenExpression {
    return BetweenExpression(
        expression = asExpression(),
        lower = wrapArgument(range.start),
        upper = wrapArgument(range.endInclusive),
        notBetween = true
    )
}

// ----- InList ------

/**
 * In-list operator, translated to the `in` keyword in SQL.
 */
public fun <T : Any> ColumnDeclaring<T>.inList(vararg list: T): InListExpression {
    return InListExpression(left = asExpression(), values = list.map { wrapArgument(it) })
}

/**
 * In-list operator, translated to the `in` keyword in SQL.
 */
public infix fun <T : Any> ColumnDeclaring<T>.inList(list: Collection<T>): InListExpression {
    return InListExpression(left = asExpression(), values = list.map { wrapArgument(it) })
}

/**
 * In-list operator, translated to the `in` keyword in SQL.
 */
public infix fun ColumnDeclaring<*>.inList(query: Query): InListExpression {
    return InListExpression(left = asExpression(), query = query.expression)
}

/**
 * Not-in-list operator, translated to the `not in` keyword in SQL.
 */
public fun <T : Any> ColumnDeclaring<T>.notInList(vararg list: T): InListExpression {
    return InListExpression(left = asExpression(), values = list.map { wrapArgument(it) }, notInList = true)
}

/**
 * Not-in-list operator, translated to the `not in` keyword in SQL.
 */
public infix fun <T : Any> ColumnDeclaring<T>.notInList(list: Collection<T>): InListExpression {
    return InListExpression(left = asExpression(), values = list.map { wrapArgument(it) }, notInList = true)
}

/**
 * Not-in-list operator, translated to the `not in` keyword in SQL.
 */
public infix fun ColumnDeclaring<*>.notInList(query: Query): InListExpression {
    return InListExpression(left = asExpression(), query = query.expression, notInList = true)
}

// ---- Exists ------

/**
 * Check if the given query has at least one result, translated to the `exists` keyword in SQL.
 */
public fun exists(query: Query): ExistsExpression {
    return ExistsExpression(query.expression)
}

/**
 * Check if the given query doesn't have any results, translated to the `not exists` keyword in SQL.
 */
public fun notExists(query: Query): ExistsExpression {
    return ExistsExpression(query.expression, notExists = true)
}

// ---- Type casting... ----

/**
 * Cast the current column or expression's type to [Double].
 */
public fun ColumnDeclaring<out Number>.toDouble(): CastingExpression<Double> {
    return this.cast(DoubleSqlType)
}

/**
 * Cast the current column or expression's type to [Float].
 */
public fun ColumnDeclaring<out Number>.toFloat(): CastingExpression<Float> {
    return this.cast(FloatSqlType)
}

/**
 * Cast the current column or expression's type to [Int].
 */
public fun ColumnDeclaring<out Number>.toInt(): CastingExpression<Int> {
    return this.cast(IntSqlType)
}

/**
 * Cast the current column or expression's type to [Short].
 */
public fun ColumnDeclaring<out Number>.toShort(): CastingExpression<Short> {
    return this.cast(ShortSqlType)
}

/**
 * Cast the current column or expression's type to [Long].
 */
public fun ColumnDeclaring<out Number>.toLong(): CastingExpression<Long> {
    return this.cast(LongSqlType)
}

/**
 * Cast the current column or expression's type to [Int].
 */
@JvmName("booleanToInt")
@Deprecated("This function will be removed in the future. ", ReplaceWith("this.cast(IntSqlType)"))
public fun ColumnDeclaring<Boolean>.toInt(): CastingExpression<Int> {
    return this.cast(IntSqlType)
}

/**
 * Cast the current column or expression to the given [SqlType].
 */
public fun <T : Any> ColumnDeclaring<*>.cast(sqlType: SqlType<T>): CastingExpression<T> {
    return CastingExpression(asExpression(), sqlType)
}
