/**
 * 该文件为 Table 及 SqlExpression 提供了扩展函数，以支持各种形式的联表
 */
package me.liuwj.ktorm.dsl

import me.liuwj.ktorm.expression.JoinExpression
import me.liuwj.ktorm.expression.JoinType
import me.liuwj.ktorm.expression.ScalarExpression
import me.liuwj.ktorm.expression.SqlExpression
import me.liuwj.ktorm.schema.Table

fun SqlExpression.crossJoin(right: SqlExpression, on: ScalarExpression<Boolean>? = null): JoinExpression {
    return JoinExpression(type = JoinType.CROSS_JOIN, left = this, right = right, condition = on)
}

fun SqlExpression.crossJoin(right: Table<*>, on: ScalarExpression<Boolean>? = null): JoinExpression {
    return crossJoin(right.asExpression(), on)
}

fun Table<*>.crossJoin(right: SqlExpression, on: ScalarExpression<Boolean>? = null): JoinExpression {
    return asExpression().crossJoin(right, on)
}

fun Table<*>.crossJoin(right: Table<*>, on: ScalarExpression<Boolean>? = null): JoinExpression {
    return crossJoin(right.asExpression(), on)
}

fun SqlExpression.innerJoin(right: SqlExpression, on: ScalarExpression<Boolean>? = null): JoinExpression {
    return JoinExpression(type = JoinType.INNER_JOIN, left = this, right = right, condition = on)
}

fun SqlExpression.innerJoin(right: Table<*>, on: ScalarExpression<Boolean>? = null): JoinExpression {
    return innerJoin(right.asExpression(), on)
}

fun Table<*>.innerJoin(right: SqlExpression, on: ScalarExpression<Boolean>? = null): JoinExpression {
    return asExpression().innerJoin(right, on)
}

fun Table<*>.innerJoin(right: Table<*>, on: ScalarExpression<Boolean>? = null): JoinExpression {
    return innerJoin(right.asExpression(), on)
}

fun SqlExpression.leftJoin(right: SqlExpression, on: ScalarExpression<Boolean>? = null): JoinExpression {
    return JoinExpression(type = JoinType.LEFT_JOIN, left = this, right = right, condition = on)
}

fun SqlExpression.leftJoin(right: Table<*>, on: ScalarExpression<Boolean>? = null): JoinExpression {
    return leftJoin(right.asExpression(), on)
}

fun Table<*>.leftJoin(right: SqlExpression, on: ScalarExpression<Boolean>? = null): JoinExpression {
    return asExpression().leftJoin(right, on)
}

fun Table<*>.leftJoin(right: Table<*>, on: ScalarExpression<Boolean>? = null): JoinExpression {
    return leftJoin(right.asExpression(), on)
}

fun SqlExpression.rightJoin(right: SqlExpression, on: ScalarExpression<Boolean>? = null): JoinExpression {
    return JoinExpression(type = JoinType.RIGHT_JOIN, left = this, right = right, condition = on)
}

fun SqlExpression.rightJoin(right: Table<*>, on: ScalarExpression<Boolean>? = null): JoinExpression {
    return rightJoin(right.asExpression(), on)
}

fun Table<*>.rightJoin(right: SqlExpression, on: ScalarExpression<Boolean>? = null): JoinExpression {
    return asExpression().rightJoin(right, on)
}

fun Table<*>.rightJoin(right: Table<*>, on: ScalarExpression<Boolean>? = null): JoinExpression {
    return rightJoin(right.asExpression(), on)
}
