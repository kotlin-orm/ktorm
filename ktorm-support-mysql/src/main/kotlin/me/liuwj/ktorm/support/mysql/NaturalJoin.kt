@file:Suppress("MatchingDeclarationName")

package me.liuwj.ktorm.support.mysql

import me.liuwj.ktorm.expression.QuerySourceExpression
import me.liuwj.ktorm.schema.Table

data class NaturalJoinExpression(
    val left: QuerySourceExpression,
    val right: QuerySourceExpression,
    override val isLeafNode: Boolean = false
) : QuerySourceExpression()

fun QuerySourceExpression.naturalJoin(right: QuerySourceExpression): NaturalJoinExpression {
    return NaturalJoinExpression(left = this, right = right)
}

fun QuerySourceExpression.naturalJoin(right: Table<*>): NaturalJoinExpression {
    return naturalJoin(right.asExpression())
}

fun Table<*>.naturalJoin(right: QuerySourceExpression): NaturalJoinExpression {
    return asExpression().naturalJoin(right)
}

fun Table<*>.naturalJoin(right: Table<*>): NaturalJoinExpression {
    return naturalJoin(right.asExpression())
}
