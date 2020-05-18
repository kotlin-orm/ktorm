/*
 * Copyright 2018-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.liuwj.ktorm.support.postgresql

import me.liuwj.ktorm.expression.ScalarExpression

/**
 * Abstract super class for PostgreSQL binary expressions.
 *
 * @property operator the expression's operator (the element between operands)
 * @property left the expression's left operand.
 * @property right the expression's right operand.
 */
abstract class BinaryExpression<LeftT : Any, RightT : Any, ReturnT : Any> : ScalarExpression<ReturnT>() {
    /**
     * Create a new instance of this expression with the two new operands and everything else the same.
     */
    abstract fun copyWithNewOperands(
        left: ScalarExpression<LeftT>,
        right: ScalarExpression<RightT>
    ): BinaryExpression<LeftT, RightT, ReturnT>
    abstract val operator: String
    abstract val left: ScalarExpression<LeftT>
    abstract val right: ScalarExpression<RightT>
}
