/**
 * 该文件为 Table 及 QuerySourceExpression 提供了扩展函数，以支持各种形式的联表
 */
package me.liuwj.ktorm.dsl

import me.liuwj.ktorm.expression.*
import me.liuwj.ktorm.schema.ColumnDeclaring
import me.liuwj.ktorm.schema.Table

fun QuerySourceExpression.crossJoin(right: QuerySourceExpression, on: ColumnDeclaring<Boolean>? = null): JoinExpression {
    return JoinExpression(type = JoinType.CROSS_JOIN, left = this, right = right, condition = on?.asExpression())
}

fun QuerySourceExpression.crossJoin(right: Table<*>, on: ColumnDeclaring<Boolean>? = null): JoinExpression {
    return crossJoin(right.asExpression(), on)
}

fun Table<*>.crossJoin(right: QuerySourceExpression, on: ColumnDeclaring<Boolean>? = null): JoinExpression {
    return asExpression().crossJoin(right, on)
}

fun Table<*>.crossJoin(right: Table<*>, on: ColumnDeclaring<Boolean>? = null): JoinExpression {
    return crossJoin(right.asExpression(), on)
}

fun QuerySourceExpression.innerJoin(right: QuerySourceExpression, on: ColumnDeclaring<Boolean>? = null): JoinExpression {
    return JoinExpression(type = JoinType.INNER_JOIN, left = this, right = right, condition = on?.asExpression())
}

fun QuerySourceExpression.innerJoin(right: Table<*>, on: ColumnDeclaring<Boolean>? = null): JoinExpression {
    return innerJoin(right.asExpression(), on)
}

fun Table<*>.innerJoin(right: QuerySourceExpression, on: ColumnDeclaring<Boolean>? = null): JoinExpression {
    return asExpression().innerJoin(right, on)
}

fun Table<*>.innerJoin(right: Table<*>, on: ColumnDeclaring<Boolean>? = null): JoinExpression {
    return innerJoin(right.asExpression(), on)
}

fun QuerySourceExpression.leftJoin(right: QuerySourceExpression, on: ColumnDeclaring<Boolean>? = null): JoinExpression {
    return JoinExpression(type = JoinType.LEFT_JOIN, left = this, right = right, condition = on?.asExpression())
}

fun QuerySourceExpression.leftJoin(right: Table<*>, on: ColumnDeclaring<Boolean>? = null): JoinExpression {
    return leftJoin(right.asExpression(), on)
}

fun Table<*>.leftJoin(right: QuerySourceExpression, on: ColumnDeclaring<Boolean>? = null): JoinExpression {
    return asExpression().leftJoin(right, on)
}

fun Table<*>.leftJoin(right: Table<*>, on: ColumnDeclaring<Boolean>? = null): JoinExpression {
    return leftJoin(right.asExpression(), on)
}

fun QuerySourceExpression.rightJoin(right: QuerySourceExpression, on: ColumnDeclaring<Boolean>? = null): JoinExpression {
    return JoinExpression(type = JoinType.RIGHT_JOIN, left = this, right = right, condition = on?.asExpression())
}

fun QuerySourceExpression.rightJoin(right: Table<*>, on: ColumnDeclaring<Boolean>? = null): JoinExpression {
    return rightJoin(right.asExpression(), on)
}

fun Table<*>.rightJoin(right: QuerySourceExpression, on: ColumnDeclaring<Boolean>? = null): JoinExpression {
    return asExpression().rightJoin(right, on)
}

fun Table<*>.rightJoin(right: Table<*>, on: ColumnDeclaring<Boolean>? = null): JoinExpression {
    return rightJoin(right.asExpression(), on)
}
