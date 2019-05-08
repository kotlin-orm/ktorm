@file:Suppress("MatchingDeclarationName")

package me.liuwj.ktorm.support.postgresql

import me.liuwj.ktorm.expression.ArgumentExpression
import me.liuwj.ktorm.expression.ScalarExpression
import me.liuwj.ktorm.schema.BooleanSqlType
import me.liuwj.ktorm.schema.ColumnDeclaring
import me.liuwj.ktorm.schema.SqlType
import me.liuwj.ktorm.schema.VarcharSqlType

data class ILikeExpression(
    val left: ScalarExpression<*>,
    val right: ScalarExpression<*>,
    override val sqlType: SqlType<Boolean> = BooleanSqlType,
    override val isLeafNode: Boolean = false
) : ScalarExpression<Boolean>()

infix fun ColumnDeclaring<*>.ilike(expr: ColumnDeclaring<String>): ILikeExpression {
    return ILikeExpression(asExpression(), expr.asExpression())
}

infix fun ColumnDeclaring<*>.ilike(argument: String): ILikeExpression {
    return this ilike ArgumentExpression(argument, VarcharSqlType)
}
