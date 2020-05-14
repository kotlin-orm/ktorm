package me.liuwj.ktorm.support.postgresql

import me.liuwj.ktorm.expression.ScalarExpression

/**
 * Abstract super class for PostgreSQL binary expressions
 *
 * @property operator the expression's operator (the element between operands)
 * @property left the expression's left operand.
 * @property right the expression's right operand.
 */
abstract class BinaryExpression<LeftT : Any, RightT : Any, ReturnT : Any> : ScalarExpression<ReturnT>() {
    /**
     * Create a new instance of this expression with the two new operands and everything else the same
     */
    abstract fun copyWithNewOperands(left: ScalarExpression<LeftT>, right: ScalarExpression<RightT>): BinaryExpression<LeftT, RightT, ReturnT>
    abstract val operator: String
    abstract val left: ScalarExpression<LeftT>
    abstract val right: ScalarExpression<RightT>
}