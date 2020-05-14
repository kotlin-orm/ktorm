package me.liuwj.ktorm.support.postgresql

import me.liuwj.ktorm.expression.ScalarExpression

/**
 * Abstract super class for PostgreSQL binary expressions
 *
 * @property operator the expression's operator (the element between operands)
 * @property left the expression's left operand.
 * @property right the expression's right operand.
 */
abstract class BinaryExpression<T : Any> : ScalarExpression<T>() {
    abstract val operator: String
    abstract val left: ScalarExpression<*>
    abstract val right: ScalarExpression<*>
}