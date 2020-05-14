package me.liuwj.ktorm.support.postgresql

import me.liuwj.ktorm.expression.ArgumentExpression
import me.liuwj.ktorm.expression.ScalarExpression
import me.liuwj.ktorm.schema.BooleanSqlType
import me.liuwj.ktorm.schema.ColumnDeclaring
import me.liuwj.ktorm.schema.SqlType
import me.liuwj.ktorm.schema.VarcharSqlType
import javax.xml.soap.Text

sealed class HstoreExpressionType<RightType : Any, ReturnType : Any>(
    val rightSqlType: SqlType<RightType>,
    val returnSqlType: SqlType<ReturnType>,
    val operator: String
)
object GetValueForKey : HstoreExpressionType<String, String>(VarcharSqlType, VarcharSqlType, "->")
object GetValuesForKey : HstoreExpressionType<Array<String>, Array<String>>(TextArraySqlType, TextArraySqlType, "->")
object Concatenate : HstoreExpressionType<Map<String, String>, Map<String, String>>(HstoreSqlType, HstoreSqlType, "||")

/**
 * Binary expression generic class for all binary operations on `hstore` types
 *
 * @property expressionType The [HstoreExpressionType] that represents this operation
 * @property left the expression's left operand.
 * @property right the expression's right operand.
 */
data class HstoreBinaryExpression<RightType : Any, ReturnType : Any>(
    val expressionType: HstoreExpressionType<RightType, ReturnType>,
    override val left: ScalarExpression<Map<String, String>>,
    override val right: ScalarExpression<RightType>,
    override val sqlType: SqlType<ReturnType> = expressionType.returnSqlType,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : BinaryExpression<ReturnType>() {
    override val operator: String = expressionType.operator
}

/**
 * Hstore getValue operator, translated to the -> operator in PostgreSQL.
 */
infix fun ColumnDeclaring<Map<String, String>>.getValue(expr: ColumnDeclaring<String>): HstoreBinaryExpression<String, String> {
    return HstoreBinaryExpression(GetValueForKey, asExpression(), expr.asExpression())
}

/**
 * Hstore get value for key operator, translated to the -> operator in PostgreSQL.
 */
infix fun ColumnDeclaring<Map<String, String>>.getValue(value: String): HstoreBinaryExpression<String, String> {
    return this getValue ArgumentExpression(value, VarcharSqlType)
}

/**
 * Hstore get values for keys for key operator, translated to the -> operator in PostgreSQL.
 */
infix fun ColumnDeclaring<Map<String, String>>.getValues(expr: ColumnDeclaring<Array<String>>): HstoreBinaryExpression<Array<String>, Array<String>> {
    return HstoreBinaryExpression(GetValuesForKey, asExpression(), expr.asExpression())
}

/**
 * Hstore get values for keys operator, translated to the -> operator in PostgreSQL.
 */
infix fun ColumnDeclaring<Map<String, String>>.getValues(value: Array<String>): HstoreBinaryExpression<Array<String>, Array<String>> {
    return this getValues ArgumentExpression(value, TextArraySqlType)
}

/**
 * Hstore concatenate operator, translated to the || operator in PostgreSQL.
 */
infix fun ColumnDeclaring<Map<String, String>>.concat(expr: ColumnDeclaring<Map<String, String>>): HstoreBinaryExpression<Map<String, String>, Map<String, String>> {
    return HstoreBinaryExpression(Concatenate, asExpression(), expr.asExpression())
}

/**
 * Hstore concatenate operator, translated to the || operator in PostgreSQL.
 */
infix fun ColumnDeclaring<Map<String, String>>.concat(value: Map<String, String>): HstoreBinaryExpression<Map<String, String>, Map<String, String>> {
    return this concat ArgumentExpression(value, HstoreSqlType)
}