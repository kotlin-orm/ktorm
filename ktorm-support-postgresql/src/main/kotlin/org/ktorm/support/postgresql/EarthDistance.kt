package org.ktorm.support.postgresql

import org.ktorm.expression.ScalarExpression
import org.ktorm.schema.BooleanSqlType
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.SqlType

public enum class CubeExpressionType(private val value: String) {
    /**
     * Cube overlaps operator, translated to the && operator in PostgreSQL.
     */
    OVERLAP("&&"),

    /**
     * Cube contains operator, translated to the @> operator in PostgreSQL.
     */
    CONTAINS("@>"),

    /**
     * Cube contained in operator, translated to the <@ operator in PostgreSQL.
     */
    CONTAINED_IN("<@");

    override fun toString(): String {
        return value
    }
}

/**
 * Expression class represents PostgreSQL's `Cube` operations.
 *
 * @property type the expression's type.
 * @property left the expression's left operand.
 * @property right the expression's right operand.
 */
public data class CubeExpression<T : Any>(
    val type: CubeExpressionType,
    val left: ScalarExpression<Cube>,
    val right: ScalarExpression<Cube>,
    override val sqlType: SqlType<T>,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : ScalarExpression<T>()

/**
 * Cube contains operator, translated to the @> operator in PostgreSQL.
 */
public infix fun ColumnDeclaring<Cube>.contains(argument: ColumnDeclaring<Cube>): CubeExpression<Boolean> {
    return CubeExpression(CubeExpressionType.CONTAINS, asExpression(), argument.asExpression(), BooleanSqlType)
}

/**
 * Cube contains operator, translated to the @> operator in PostgreSQL.
 */
public infix fun ColumnDeclaring<Cube>.contains(argument: Cube): CubeExpression<Boolean> {
    return this.contains(wrapArgument(argument))
}

/**
 * Cube contained in operator, translated to the <@ operator in PostgreSQL.
 */
public infix fun ColumnDeclaring<Cube>.containedIn(argument: ColumnDeclaring<Cube>): CubeExpression<Boolean> {
    return CubeExpression(CubeExpressionType.CONTAINED_IN, asExpression(), argument.asExpression(), BooleanSqlType)
}

/**
 * Cube contained in operator, translated to the <@ operator in PostgreSQL.
 */
public infix fun ColumnDeclaring<Cube>.containedIn(argument: Cube): CubeExpression<Boolean> {
    return this.containedIn(wrapArgument(argument))
}

/**
 * Cube overlap operator, translated to the && operator in PostgreSQL.
 */
public infix fun ColumnDeclaring<Cube>.overlaps(argument: ColumnDeclaring<Cube>): CubeExpression<Boolean> {
    return CubeExpression(CubeExpressionType.OVERLAP, asExpression(), argument.asExpression(), BooleanSqlType)
}

/**
 * Cube overlap operator, translated to the && operator in PostgreSQL.
 */
public infix fun ColumnDeclaring<Cube>.overlaps(argument: Cube): CubeExpression<Boolean> {
    return this.overlaps(wrapArgument(argument))
}