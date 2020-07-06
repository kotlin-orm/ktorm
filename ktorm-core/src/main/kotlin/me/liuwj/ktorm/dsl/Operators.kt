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

import me.liuwj.ktorm.expression.*
import me.liuwj.ktorm.schema.*

// ---- Unary operators... ----

/**
 * Check if the current column or expression is null, translated to `is null` in SQL.
 */
fun ColumnDeclaring<*>.isNull(): UnaryExpression<Boolean> {
    return UnaryExpression(UnaryExpressionType.IS_NULL, asExpression(), BooleanSqlType)
}

/**
 * Check if the current column or expression is not null, translated to `is not null` in SQL.
 */
fun ColumnDeclaring<*>.isNotNull(): UnaryExpression<Boolean> {
    return UnaryExpression(UnaryExpressionType.IS_NOT_NULL, asExpression(), BooleanSqlType)
}

/**
 * Unary minus operator, translated to `-` in SQL.
 */
operator fun <T : Number> ColumnDeclaring<T>.unaryMinus(): UnaryExpression<T> {
    return UnaryExpression(UnaryExpressionType.UNARY_MINUS, asExpression(), sqlType)
}

/**
 * Unary plus operator, translated to `+` in SQL.
 */
operator fun <T : Number> ColumnDeclaring<T>.unaryPlus(): UnaryExpression<T> {
    return UnaryExpression(UnaryExpressionType.UNARY_PLUS, asExpression(), sqlType)
}

/**
 * Negative operator, translated to the `not` keyword in SQL.
 */
operator fun ColumnDeclaring<Boolean>.not(): UnaryExpression<Boolean> {
    return UnaryExpression(UnaryExpressionType.NOT, asExpression(), BooleanSqlType)
}

// ---- Plus(+) ----

/**
 * Plus operator, translated to `+` in SQL.
 */
infix operator fun <T : Number> ColumnDeclaring<T>.plus(expr: ColumnDeclaring<T>): BinaryExpression<T> {
    return BinaryExpression(BinaryExpressionType.PLUS, asExpression(), expr.asExpression(), sqlType)
}

/**
 * Plus operator, translated to `+` in SQL.
 */
infix operator fun <T : Number> ColumnDeclaring<T>.plus(argument: T): BinaryExpression<T> {
    return this + wrapArgument(argument)
}

/**
 * Plus operator, translated to `+` in SQL.
 */
infix operator fun <T : Number> T.plus(expr: ColumnDeclaring<T>): BinaryExpression<T> {
    return expr.wrapArgument(this) + expr
}

// ------- Minus(-) -----------

/**
 * Minus operator, translated to `-` in SQL.
 */
infix operator fun <T : Number> ColumnDeclaring<T>.minus(expr: ColumnDeclaring<T>): BinaryExpression<T> {
    return BinaryExpression(BinaryExpressionType.MINUS, asExpression(), expr.asExpression(), sqlType)
}

/**
 * Minus operator, translated to `-` in SQL.
 */
infix operator fun <T : Number> ColumnDeclaring<T>.minus(argument: T): BinaryExpression<T> {
    return this - wrapArgument(argument)
}

/**
 * Minus operator, translated to `-` in SQL.
 */
infix operator fun <T : Number> T.minus(expr: ColumnDeclaring<T>): BinaryExpression<T> {
    return expr.wrapArgument(this) - expr
}

// -------- Times(*) -----------

/**
 * Multiply operator, translated to `*` in SQL.
 */
infix operator fun <T : Number> ColumnDeclaring<T>.times(expr: ColumnDeclaring<T>): BinaryExpression<T> {
    return BinaryExpression(BinaryExpressionType.TIMES, asExpression(), expr.asExpression(), sqlType)
}

/**
 * Multiply operator, translated to `*` in SQL.
 */
infix operator fun <T : Number> ColumnDeclaring<T>.times(argument: T): BinaryExpression<T> {
    return this * wrapArgument(argument)
}

/**
 * Multiply operator, translated to `*` in SQL.
 */
infix operator fun <T : Number> T.times(expr: ColumnDeclaring<T>): BinaryExpression<T> {
    return expr.wrapArgument(this) * expr
}

// -------- Div(/) ----------

/**
 * Divide operator, translated to `/` in SQL.
 */
infix operator fun <T : Number> ColumnDeclaring<T>.div(expr: ColumnDeclaring<T>): BinaryExpression<T> {
    return BinaryExpression(BinaryExpressionType.DIV, asExpression(), expr.asExpression(), sqlType)
}

/**
 * Divide operator, translated to `/` in SQL.
 */
infix operator fun <T : Number> ColumnDeclaring<T>.div(argument: T): BinaryExpression<T> {
    return this / wrapArgument(argument)
}

/**
 * Divide operator, translated to `/` in SQL.
 */
infix operator fun <T : Number> T.div(expr: ColumnDeclaring<T>): BinaryExpression<T> {
    return expr.wrapArgument(this) / expr
}

// -------- Rem(%) ----------

/**
 * Mod operator, translated to `%` in SQL.
 */
infix operator fun <T : Number> ColumnDeclaring<T>.rem(expr: ColumnDeclaring<T>): BinaryExpression<T> {
    return BinaryExpression(BinaryExpressionType.REM, asExpression(), expr.asExpression(), sqlType)
}

/**
 * Mod operator, translated to `%` in SQL.
 */
infix operator fun <T : Number> ColumnDeclaring<T>.rem(argument: T): BinaryExpression<T> {
    return this % wrapArgument(argument)
}

/**
 * Mod operator, translated to `%` in SQL.
 */
infix operator fun <T : Number> T.rem(expr: ColumnDeclaring<T>): BinaryExpression<T> {
    return expr.wrapArgument(this) % expr
}

// -------- Like ----------

/**
 * Like operator, translated to the `like` keyword in SQL.
 */
infix fun ColumnDeclaring<*>.like(expr: ColumnDeclaring<String>): BinaryExpression<Boolean> {
    return BinaryExpression(BinaryExpressionType.LIKE, asExpression(), expr.asExpression(), BooleanSqlType)
}

/**
 * Like operator, translated to the `like` keyword in SQL.
 */
infix fun ColumnDeclaring<*>.like(argument: String): BinaryExpression<Boolean> {
    return this like ArgumentExpression(argument, VarcharSqlType)
}

/**
 * Not like operator, translated to the `not like` keyword in SQL.
 */
infix fun ColumnDeclaring<*>.notLike(expr: ColumnDeclaring<String>): BinaryExpression<Boolean> {
    return BinaryExpression(BinaryExpressionType.NOT_LIKE, asExpression(), expr.asExpression(), BooleanSqlType)
}

/**
 * Not like operator, translated to the `not like` keyword in SQL.
 */
infix fun ColumnDeclaring<*>.notLike(argument: String): BinaryExpression<Boolean> {
    return this notLike ArgumentExpression(argument, VarcharSqlType)
}

// --------- And ------------

/**
 * And operator, translated to the `and` keyword in SQL.
 */
infix fun ColumnDeclaring<Boolean>.and(expr: ColumnDeclaring<Boolean>): BinaryExpression<Boolean> {
    return BinaryExpression(BinaryExpressionType.AND, asExpression(), expr.asExpression(), BooleanSqlType)
}

/**
 * And operator, translated to the `and` keyword in SQL.
 */
infix fun ColumnDeclaring<Boolean>.and(argument: Boolean): BinaryExpression<Boolean> {
    return this and wrapArgument(argument)
}

/**
 * And operator, translated to the `and` keyword in SQL.
 */
infix fun Boolean.and(expr: ColumnDeclaring<Boolean>): BinaryExpression<Boolean> {
    return expr.wrapArgument(this) and expr
}

// --------- Or ----------

/**
 * Or operator, translated to the `or` keyword in SQL.
 */
infix fun ColumnDeclaring<Boolean>.or(expr: ColumnDeclaring<Boolean>): BinaryExpression<Boolean> {
    return BinaryExpression(BinaryExpressionType.OR, asExpression(), expr.asExpression(), BooleanSqlType)
}

/**
 * Or operator, translated to the `or` keyword in SQL.
 */
infix fun ColumnDeclaring<Boolean>.or(argument: Boolean): BinaryExpression<Boolean> {
    return this or wrapArgument(argument)
}

/**
 * Or operator, translated to the `or` keyword in SQL.
 */
infix fun Boolean.or(expr: ColumnDeclaring<Boolean>): BinaryExpression<Boolean> {
    return expr.wrapArgument(this) or expr
}

// -------- Xor ---------

/**
 * Xor operator, translated to the `xor` keyword in SQL.
 */
infix fun ColumnDeclaring<Boolean>.xor(expr: ColumnDeclaring<Boolean>): BinaryExpression<Boolean> {
    return BinaryExpression(BinaryExpressionType.XOR, asExpression(), expr.asExpression(), BooleanSqlType)
}

/**
 * Xor operator, translated to the `xor` keyword in SQL.
 */
infix fun ColumnDeclaring<Boolean>.xor(argument: Boolean): BinaryExpression<Boolean> {
    return this xor wrapArgument(argument)
}

/**
 * Xor operator, translated to the `xor` keyword in SQL.
 */
infix fun Boolean.xor(expr: ColumnDeclaring<Boolean>): BinaryExpression<Boolean> {
    return expr.wrapArgument(this) xor expr
}

// ------- Less --------

/**
 * Less operator, translated to `<` in SQL.
 */
infix fun <T : Comparable<T>> ColumnDeclaring<T>.less(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return BinaryExpression(BinaryExpressionType.LESS_THAN, asExpression(), expr.asExpression(), BooleanSqlType)
}

/**
 * Less operator, translated to `<` in SQL.
 */
infix fun <T : Comparable<T>> ColumnDeclaring<T>.less(argument: T): BinaryExpression<Boolean> {
    return this less wrapArgument(argument)
}

/**
 * Less operator, translated to `<` in SQL.
 */
infix fun <T : Comparable<T>> T.less(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return expr.wrapArgument(this) less expr
}

// ------- LessEq ---------

/**
 * Less-eq operator, translated to `<=` in SQL.
 */
infix fun <T : Comparable<T>> ColumnDeclaring<T>.lessEq(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
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
infix fun <T : Comparable<T>> ColumnDeclaring<T>.lessEq(argument: T): BinaryExpression<Boolean> {
    return this lessEq wrapArgument(argument)
}

/**
 * Less-eq operator, translated to `<=` in SQL.
 */
infix fun <T : Comparable<T>> T.lessEq(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return expr.wrapArgument(this) lessEq expr
}

// ------- Greater ---------

/**
 * Greater operator, translated to `>` in SQL.
 */
infix fun <T : Comparable<T>> ColumnDeclaring<T>.greater(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return BinaryExpression(BinaryExpressionType.GREATER_THAN, asExpression(), expr.asExpression(), BooleanSqlType)
}

/**
 * Greater operator, translated to `>` in SQL.
 */
infix fun <T : Comparable<T>> ColumnDeclaring<T>.greater(argument: T): BinaryExpression<Boolean> {
    return this greater wrapArgument(argument)
}

/**
 * Greater operator, translated to `>` in SQL.
 */
infix fun <T : Comparable<T>> T.greater(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return expr.wrapArgument(this) greater expr
}

// -------- GreaterEq ---------

/**
 * Greater-eq operator, translated to `>=` in SQL.
 */
infix fun <T : Comparable<T>> ColumnDeclaring<T>.greaterEq(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
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
infix fun <T : Comparable<T>> ColumnDeclaring<T>.greaterEq(argument: T): BinaryExpression<Boolean> {
    return this greaterEq wrapArgument(argument)
}

/**
 * Greater-eq operator, translated to `>=` in SQL.
 */
infix fun <T : Comparable<T>> T.greaterEq(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return expr.wrapArgument(this) greaterEq expr
}

// -------- Eq ---------

/**
 * Equal operator, translated to `=` in SQL.
 */
infix fun <T : Any> ColumnDeclaring<T>.eq(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return BinaryExpression(BinaryExpressionType.EQUAL, asExpression(), expr.asExpression(), BooleanSqlType)
}

/**
 * Equal operator, translated to `=` in SQL.
 */
infix fun <T : Any> ColumnDeclaring<T>.eq(argument: T): BinaryExpression<Boolean> {
    return this eq wrapArgument(argument)
}

// infix fun <T : Any> T.eq(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
//     return expr.wrapArgument(this) eq expr
// }

// ------- NotEq -------

/**
 * Not-equal operator, translated to `<>` in SQL.
 */
infix fun <T : Any> ColumnDeclaring<T>.notEq(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return BinaryExpression(BinaryExpressionType.NOT_EQUAL, asExpression(), expr.asExpression(), BooleanSqlType)
}

/**
 * Not-equal operator, translated to `<>` in SQL.
 */
infix fun <T : Any> ColumnDeclaring<T>.notEq(argument: T): BinaryExpression<Boolean> {
    return this notEq wrapArgument(argument)
}

// infix fun <T : Any> T.notEq(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
//     return expr.wrapArgument(this) notEq expr
// }

// ---- Between ----

/**
 * Between operator, translated to `between .. and ..` in SQL.
 */
infix fun <T : Comparable<T>> ColumnDeclaring<T>.between(range: ClosedRange<T>): BetweenExpression<T> {
    return BetweenExpression(asExpression(), wrapArgument(range.start), wrapArgument(range.endInclusive))
}

/**
 * Not-between operator, translated to `not between .. and ..` in SQL.
 */
infix fun <T : Comparable<T>> ColumnDeclaring<T>.notBetween(range: ClosedRange<T>): BetweenExpression<T> {
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
fun <T : Any> ColumnDeclaring<T>.inList(vararg list: T): InListExpression<T> {
    return InListExpression(left = asExpression(), values = list.map { wrapArgument(it) })
}

/**
 * In-list operator, translated to the `in` keyword in SQL.
 */
infix fun <T : Any> ColumnDeclaring<T>.inList(list: Collection<T>): InListExpression<T> {
    return InListExpression(left = asExpression(), values = list.map { wrapArgument(it) })
}

/**
 * In-list operator, translated to the `in` keyword in SQL.
 */
infix fun <T : Any> ColumnDeclaring<T>.inList(query: Query): InListExpression<T> {
    return InListExpression(left = asExpression(), query = query.expression)
}

/**
 * Not-in-list operator, translated to the `not in` keyword in SQL.
 */
fun <T : Any> ColumnDeclaring<T>.notInList(vararg list: T): InListExpression<T> {
    return InListExpression(left = asExpression(), values = list.map { wrapArgument(it) }, notInList = true)
}

/**
 * Not-in-list operator, translated to the `not in` keyword in SQL.
 */
infix fun <T : Any> ColumnDeclaring<T>.notInList(list: Collection<T>): InListExpression<T> {
    return InListExpression(left = asExpression(), values = list.map { wrapArgument(it) }, notInList = true)
}

/**
 * Not-in-list operator, translated to the `not in` keyword in SQL.
 */
infix fun <T : Any> ColumnDeclaring<T>.notInList(query: Query): InListExpression<T> {
    return InListExpression(left = asExpression(), query = query.expression, notInList = true)
}

// ---- Exists ------

/**
 * Check if the given query has at least one result, translated to the `exists` keyword in SQL.
 */
fun exists(query: Query): ExistsExpression {
    return ExistsExpression(query.expression)
}

/**
 * Check if the given query doesn't have any results, translated to the `not exists` keyword in SQL.
 */
fun notExists(query: Query): ExistsExpression {
    return ExistsExpression(query.expression, notExists = true)
}

// ---- Type casting... ----

/**
 * Cast the current column or expression's type to [Double].
 */
fun ColumnDeclaring<out Number>.toDouble(): CastingExpression<Double> {
    return this.cast(DoubleSqlType)
}

/**
 * Cast the current column or expression's type to [Float].
 */
fun ColumnDeclaring<out Number>.toFloat(): CastingExpression<Float> {
    return this.cast(FloatSqlType)
}

/**
 * Cast the current column or expression's type to [Int].
 */
@JvmName("toIntWithColumnDeclaringNumber")
fun ColumnDeclaring<Number>.toInt(): CastingExpression<Int> {
    return this.cast(IntSqlType)
}

/**
 * Cast the current column or expression's type to [Int].
 */
@JvmName("toIntWithColumnDeclaringBoolean")
fun ColumnDeclaring<Boolean>.toInt(): CastingExpression<Int> {
    return this.cast(IntSqlType)
}

/**
 * Cast the current column or expression's type to [Long].
 */
fun ColumnDeclaring<out Number>.toLong(): CastingExpression<Long> {
    return this.cast(LongSqlType)
}

/**
 * Cast the current column or expression to the given [SqlType].
 */
fun <T : Any> ColumnDeclaring<*>.cast(sqlType: SqlType<T>): CastingExpression<T> {
    return CastingExpression(asExpression(), sqlType)
}
