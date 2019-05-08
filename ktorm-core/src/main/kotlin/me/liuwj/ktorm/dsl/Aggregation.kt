package me.liuwj.ktorm.dsl

import me.liuwj.ktorm.entity.*
import me.liuwj.ktorm.expression.AggregateExpression
import me.liuwj.ktorm.expression.AggregateType
import me.liuwj.ktorm.schema.ColumnDeclaring
import me.liuwj.ktorm.schema.DoubleSqlType
import me.liuwj.ktorm.schema.IntSqlType
import me.liuwj.ktorm.schema.Table

fun <C : Number> min(column: ColumnDeclaring<C>): AggregateExpression<C> {
    return AggregateExpression(AggregateType.MIN, column.asExpression(), false, column.sqlType)
}

fun <C : Number> minDistinct(column: ColumnDeclaring<C>): AggregateExpression<C> {
    return AggregateExpression(AggregateType.MIN, column.asExpression(), true, column.sqlType)
}

fun <C : Number> max(column: ColumnDeclaring<C>): AggregateExpression<C> {
    return AggregateExpression(AggregateType.MAX, column.asExpression(), false, column.sqlType)
}

fun <C : Number> maxDistinct(column: ColumnDeclaring<C>): AggregateExpression<C> {
    return AggregateExpression(AggregateType.MAX, column.asExpression(), true, column.sqlType)
}

fun <C : Number> avg(column: ColumnDeclaring<C>): AggregateExpression<Double> {
    return AggregateExpression(AggregateType.AVG, column.asExpression(), false, DoubleSqlType)
}

fun <C : Number> avgDistinct(column: ColumnDeclaring<C>): AggregateExpression<Double> {
    return AggregateExpression(AggregateType.AVG, column.asExpression(), true, DoubleSqlType)
}

fun <C : Number> sum(column: ColumnDeclaring<C>): AggregateExpression<C> {
    return AggregateExpression(AggregateType.SUM, column.asExpression(), false, column.sqlType)
}

fun <C : Number> sumDistinct(column: ColumnDeclaring<C>): AggregateExpression<C> {
    return AggregateExpression(AggregateType.SUM, column.asExpression(), true, column.sqlType)
}

fun count(column: ColumnDeclaring<*>? = null): AggregateExpression<Int> {
    return AggregateExpression(AggregateType.COUNT, column?.asExpression(), false, IntSqlType)
}

fun countDistinct(column: ColumnDeclaring<*>? = null): AggregateExpression<Int> {
    return AggregateExpression(AggregateType.COUNT, column?.asExpression(), true, IntSqlType)
}

/**
 * 如果表中的所有行都符合指定条件，返回 true，否则 false
 */
inline fun <E : Entity<E>, T : Table<E>> T.all(predicate: (T) -> ColumnDeclaring<Boolean>): Boolean {
    return asSequence().all(predicate)
}

/**
 * 如果表中有数据，返回 true，否则 false
 */
fun <E : Entity<E>, T : Table<E>> T.any(): Boolean {
    return asSequence().any()
}

/**
 * 如果表中存在任何一条记录满足指定条件，返回 true，否则 false
 */
inline fun <E : Entity<E>, T : Table<E>> T.any(predicate: (T) -> ColumnDeclaring<Boolean>): Boolean {
    return asSequence().any(predicate)
}

/**
 * 如果表中没有数据，返回 true，否则 false
 */
fun <E : Entity<E>, T : Table<E>> T.none(): Boolean {
    return asSequence().none()
}

/**
 * 如果表中所有记录都不满足指定条件，返回 true，否则 false
 */
inline fun <E : Entity<E>, T : Table<E>> T.none(predicate: (T) -> ColumnDeclaring<Boolean>): Boolean {
    return asSequence().none(predicate)
}

/**
 * 返回表中的记录数
 */
fun <E : Entity<E>, T : Table<E>> T.count(): Int {
    return asSequence().count()
}

/**
 * 返回表中满足指定条件的记录数
 */
inline fun <E : Entity<E>, T : Table<E>> T.count(predicate: (T) -> ColumnDeclaring<Boolean>): Int {
    return asSequence().count(predicate)
}

/**
 * 返回表中指定字段的和，若表中没有数据，返回 null
 */
inline fun <E : Entity<E>, T : Table<E>, C : Number> T.sumBy(selector: (T) -> ColumnDeclaring<C>): C? {
    return asSequence().sumBy(selector)
}

/**
 * 返回表中指定字段的最大值，若表中没有数据，返回 null
 */
inline fun <E : Entity<E>, T : Table<E>, C : Number> T.maxBy(selector: (T) -> ColumnDeclaring<C>): C? {
    return asSequence().maxBy(selector)
}

/**
 * 返回表中指定字段的最小值，若表中没有数据，返回 null
 */
inline fun <E : Entity<E>, T : Table<E>, C : Number> T.minBy(selector: (T) -> ColumnDeclaring<C>): C? {
    return asSequence().minBy(selector)
}

/**
 * 返回表中指定字段的平均值，若表中没有数据，返回 null
 */
inline fun <E : Entity<E>, T : Table<E>> T.averageBy(selector: (T) -> ColumnDeclaring<out Number>): Double? {
    return asSequence().averageBy(selector)
}
