/**
 * 该文件为 Column 及 SqlExpression 提供扩展函数，以支持各种操作符
 */
package me.liuwj.ktorm.dsl

import me.liuwj.ktorm.expression.*
import me.liuwj.ktorm.schema.*

// ---- Unary operators... ----

fun ColumnDeclaring<*>.isNull(): UnaryExpression<Boolean> {
    return UnaryExpression(UnaryExpressionType.IS_NULL, asExpression(), BooleanSqlType)
}

fun ColumnDeclaring<*>.isNotNull(): UnaryExpression<Boolean> {
    return UnaryExpression(UnaryExpressionType.IS_NOT_NULL, asExpression(), BooleanSqlType)
}

operator fun <T : Number> ColumnDeclaring<T>.unaryMinus(): UnaryExpression<T> {
    return UnaryExpression(UnaryExpressionType.UNARY_MINUS, asExpression(), sqlType)
}

operator fun <T : Number> ColumnDeclaring<T>.unaryPlus(): UnaryExpression<T> {
    return UnaryExpression(UnaryExpressionType.UNARY_PLUS, asExpression(), sqlType)
}

operator fun ColumnDeclaring<Boolean>.not(): UnaryExpression<Boolean> {
    return UnaryExpression(UnaryExpressionType.NOT, asExpression(), BooleanSqlType)
}

// ---- Plus(+) ----

infix operator fun <T : Number> ColumnDeclaring<T>.plus(expr: ColumnDeclaring<T>): BinaryExpression<T> {
    return BinaryExpression(BinaryExpressionType.PLUS, asExpression(), expr.asExpression(), sqlType)
}

infix operator fun <T : Number> ColumnDeclaring<T>.plus(argument: T): BinaryExpression<T> {
    return this + wrapArgument(argument)
}

infix operator fun <T : Number> T.plus(expr: ColumnDeclaring<T>): BinaryExpression<T> {
    return expr.wrapArgument(this) + expr
}

// ------- Minus(-) -----------

infix operator fun <T : Number> ColumnDeclaring<T>.minus(expr: ColumnDeclaring<T>): BinaryExpression<T> {
    return BinaryExpression(BinaryExpressionType.MINUS, asExpression(), expr.asExpression(), sqlType)
}

infix operator fun <T : Number> ColumnDeclaring<T>.minus(argument: T): BinaryExpression<T> {
    return this - wrapArgument(argument)
}

infix operator fun <T : Number> T.minus(expr: ColumnDeclaring<T>): BinaryExpression<T> {
    return expr.wrapArgument(this) - expr
}

// -------- Times(*) -----------

infix operator fun <T : Number> ColumnDeclaring<T>.times(expr: ColumnDeclaring<T>): BinaryExpression<T> {
    return BinaryExpression(BinaryExpressionType.TIMES, asExpression(), expr.asExpression(), sqlType)
}

infix operator fun <T : Number> ColumnDeclaring<T>.times(argument: T): BinaryExpression<T> {
    return this * wrapArgument(argument)
}

infix operator fun <T : Number> T.times(expr: ColumnDeclaring<T>): BinaryExpression<T> {
    return expr.wrapArgument(this) * expr
}

// -------- Div(/) ----------

infix operator fun <T : Number> ColumnDeclaring<T>.div(expr: ColumnDeclaring<T>): BinaryExpression<T> {
    return BinaryExpression(BinaryExpressionType.DIV, asExpression(), expr.asExpression(), sqlType)
}

infix operator fun <T : Number> ColumnDeclaring<T>.div(argument: T): BinaryExpression<T> {
    return this / wrapArgument(argument)
}

infix operator fun <T : Number> T.div(expr: ColumnDeclaring<T>): BinaryExpression<T> {
    return expr.wrapArgument(this) / expr
}

// -------- Rem(%) ----------

infix operator fun <T : Number> ColumnDeclaring<T>.rem(expr: ColumnDeclaring<T>): BinaryExpression<T> {
    return BinaryExpression(BinaryExpressionType.REM, asExpression(), expr.asExpression(), sqlType)
}

infix operator fun <T : Number> ColumnDeclaring<T>.rem(argument: T): BinaryExpression<T> {
    return this % wrapArgument(argument)
}

infix operator fun <T : Number> T.rem(expr: ColumnDeclaring<T>): BinaryExpression<T> {
    return expr.wrapArgument(this) % expr
}

// -------- Like ----------

infix fun ColumnDeclaring<*>.like(expr: ColumnDeclaring<String>): BinaryExpression<Boolean> {
    return BinaryExpression(BinaryExpressionType.LIKE, asExpression(), expr.asExpression(), BooleanSqlType)
}

infix fun ColumnDeclaring<*>.like(argument: String): BinaryExpression<Boolean> {
    return this like ArgumentExpression(argument, VarcharSqlType)
}

infix fun ColumnDeclaring<*>.notLike(expr: ColumnDeclaring<String>): BinaryExpression<Boolean> {
    return BinaryExpression(BinaryExpressionType.NOT_LIKE, asExpression(), expr.asExpression(), BooleanSqlType)
}

infix fun ColumnDeclaring<*>.notLike(argument: String): BinaryExpression<Boolean> {
    return this notLike ArgumentExpression(argument, VarcharSqlType)
}

// --------- And ------------

infix fun ColumnDeclaring<Boolean>.and(expr: ColumnDeclaring<Boolean>): BinaryExpression<Boolean> {
    return BinaryExpression(BinaryExpressionType.AND, asExpression(), expr.asExpression(), BooleanSqlType)
}

infix fun ColumnDeclaring<Boolean>.and(argument: Boolean): BinaryExpression<Boolean> {
    return this and wrapArgument(argument)
}

infix fun Boolean.and(expr: ColumnDeclaring<Boolean>): BinaryExpression<Boolean> {
    return expr.wrapArgument(this) and expr
}

// --------- Or ----------

infix fun ColumnDeclaring<Boolean>.or(expr: ColumnDeclaring<Boolean>): BinaryExpression<Boolean> {
    return BinaryExpression(BinaryExpressionType.OR, asExpression(), expr.asExpression(), BooleanSqlType)
}

infix fun ColumnDeclaring<Boolean>.or(argument: Boolean): BinaryExpression<Boolean> {
    return this or wrapArgument(argument)
}

infix fun Boolean.or(expr: ColumnDeclaring<Boolean>): BinaryExpression<Boolean> {
    return expr.wrapArgument(this) or expr
}

// -------- Xor ---------

infix fun ColumnDeclaring<Boolean>.xor(expr: ColumnDeclaring<Boolean>): BinaryExpression<Boolean> {
    return BinaryExpression(BinaryExpressionType.XOR, asExpression(), expr.asExpression(), BooleanSqlType)
}

infix fun ColumnDeclaring<Boolean>.xor(argument: Boolean): BinaryExpression<Boolean> {
    return this xor wrapArgument(argument)
}

infix fun Boolean.xor(expr: ColumnDeclaring<Boolean>): BinaryExpression<Boolean> {
    return expr.wrapArgument(this) xor expr
}

// ------- Less --------

infix fun <T : Comparable<T>> ColumnDeclaring<T>.less(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return BinaryExpression(BinaryExpressionType.LESS_THAN, asExpression(), expr.asExpression(), BooleanSqlType)
}

infix fun <T : Comparable<T>> ColumnDeclaring<T>.less(argument: T): BinaryExpression<Boolean> {
    return this less wrapArgument(argument)
}

infix fun <T : Comparable<T>> T.less(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return expr.wrapArgument(this) less expr
}

// ------- LessEq ---------

infix fun <T : Comparable<T>> ColumnDeclaring<T>.lessEq(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return BinaryExpression(
        type = BinaryExpressionType.LESS_THAN_OR_EQUAL,
        left = asExpression(),
        right = expr.asExpression(),
        sqlType = BooleanSqlType
    )
}

infix fun <T : Comparable<T>> ColumnDeclaring<T>.lessEq(argument: T): BinaryExpression<Boolean> {
    return this lessEq wrapArgument(argument)
}

infix fun <T : Comparable<T>> T.lessEq(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return expr.wrapArgument(this) lessEq expr
}

// ------- Greater ---------

infix fun <T : Comparable<T>> ColumnDeclaring<T>.greater(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return BinaryExpression(BinaryExpressionType.GREATER_THAN, asExpression(), expr.asExpression(), BooleanSqlType)
}

infix fun <T : Comparable<T>> ColumnDeclaring<T>.greater(argument: T): BinaryExpression<Boolean> {
    return this greater wrapArgument(argument)
}

infix fun <T : Comparable<T>> T.greater(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return expr.wrapArgument(this) greater expr
}

// -------- GreaterEq ---------

infix fun <T : Comparable<T>> ColumnDeclaring<T>.greaterEq(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return BinaryExpression(
        type = BinaryExpressionType.GREATER_THAN_OR_EQUAL,
        left = asExpression(),
        right = expr.asExpression(),
        sqlType = BooleanSqlType
    )
}

infix fun <T : Comparable<T>> ColumnDeclaring<T>.greaterEq(argument: T): BinaryExpression<Boolean> {
    return this greaterEq wrapArgument(argument)
}

infix fun <T : Comparable<T>> T.greaterEq(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return expr.wrapArgument(this) greaterEq expr
}

// -------- Eq ---------

infix fun <T : Any> ColumnDeclaring<T>.eq(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return BinaryExpression(BinaryExpressionType.EQUAL, asExpression(), expr.asExpression(), BooleanSqlType)
}

infix fun <T : Any> ColumnDeclaring<T>.eq(argument: T): BinaryExpression<Boolean> {
    return this eq wrapArgument(argument)
}

infix fun <T : Any> T.eq(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return expr.wrapArgument(this) eq expr
}

// ------- NotEq -------

infix fun <T : Any> ColumnDeclaring<T>.notEq(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return BinaryExpression(BinaryExpressionType.NOT_EQUAL, asExpression(), expr.asExpression(), BooleanSqlType)
}

infix fun <T : Any> ColumnDeclaring<T>.notEq(argument: T): BinaryExpression<Boolean> {
    return this notEq wrapArgument(argument)
}

infix fun <T : Any> T.notEq(expr: ColumnDeclaring<T>): BinaryExpression<Boolean> {
    return expr.wrapArgument(this) notEq expr
}

// ---- Between ----

infix fun <T : Comparable<T>> ColumnDeclaring<T>.between(range: ClosedRange<T>): BetweenExpression<T> {
    return BetweenExpression(asExpression(), wrapArgument(range.start), wrapArgument(range.endInclusive))
}

infix fun <T : Comparable<T>> ColumnDeclaring<T>.notBetween(range: ClosedRange<T>): BetweenExpression<T> {
    return BetweenExpression(
        expression = asExpression(),
        lower = wrapArgument(range.start),
        upper = wrapArgument(range.endInclusive),
        notBetween = true
    )
}

// ----- InList ------

infix fun <T : Any> ColumnDeclaring<T>.inList(collection: Collection<T>): InListExpression<T> {
    return InListExpression(left = asExpression(), values = collection.map { wrapArgument(it) })
}

infix fun <T : Any> ColumnDeclaring<T>.inList(query: Query): InListExpression<T> {
    return InListExpression(left = asExpression(), query = query.expression)
}

infix fun <T : Any> ColumnDeclaring<T>.notInList(collection: Collection<T>): InListExpression<T> {
    return InListExpression(left = asExpression(), values = collection.map { wrapArgument(it) }, notInList = true)
}

infix fun <T : Any> ColumnDeclaring<T>.notInList(query: Query): InListExpression<T> {
    return InListExpression(left = asExpression(), query = query.expression, notInList = true)
}

// ---- Exists ------

fun exists(query: Query): ExistsExpression {
    return ExistsExpression(query.expression)
}

fun notExists(query: Query): ExistsExpression {
    return ExistsExpression(query.expression, notExists = true)
}

// ---- Type casting... ----

fun ColumnDeclaring<out Number>.toDouble(): CastingExpression<Double> {
    return this.cast(DoubleSqlType)
}

fun ColumnDeclaring<out Number>.toFloat(): CastingExpression<Float> {
    return this.cast(FloatSqlType)
}

fun ColumnDeclaring<out Number>.toInt(): CastingExpression<Int> {
    return this.cast(IntSqlType)
}

fun ColumnDeclaring<out Number>.toLong(): CastingExpression<Long> {
    return this.cast(LongSqlType)
}

fun <T : Any> ColumnDeclaring<*>.cast(sqlType: SqlType<T>): CastingExpression<T> {
    return CastingExpression(asExpression(), sqlType)
}
