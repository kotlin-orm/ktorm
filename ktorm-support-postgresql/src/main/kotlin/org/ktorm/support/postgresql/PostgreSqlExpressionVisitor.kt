/*
 * Copyright 2018-2023 the original author or authors.
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

package org.ktorm.support.postgresql

import org.ktorm.expression.*

/**
 * Base interface designed to visit or modify PostgreSQL expression trees using visitor pattern.
 *
 * For detailed documents, see [SqlExpressionVisitor].
 */
public interface PostgreSqlExpressionVisitor : SqlExpressionVisitor {

    /**
     * Dispatch different type of expression nodes to their specific `visit*` functions. Custom expression types that
     * are unknown to Ktorm will be dispatched to [visitUnknown].
     */
    override fun visit(expr: SqlExpression): SqlExpression {
        return when (expr) {
            is InsertOrUpdateExpression -> visitInsertOrUpdate(expr)
            is BulkInsertExpression -> visitBulkInsert(expr)
            else -> super.visit(expr)
        }
    }

    /**
     * Function that visits a general [ScalarExpression], this function dispatches different type of scalar expressions
     * to their specific `visit*` functions. Custom expression types that are unknown to Ktorm will be dispatched to
     * [visitUnknown]
     */
    override fun <T : Any> visitScalar(expr: ScalarExpression<T>): ScalarExpression<T> {
        val result = when (expr) {
            is ILikeExpression -> visitILike(expr)
            is HStoreExpression -> visitHStore(expr)
            is CubeExpression -> visitCube(expr)
            is DefaultValueExpression -> visitDefaultValue(expr)
            else -> super.visitScalar(expr)
        }

        @Suppress("UNCHECKED_CAST")
        return result as ScalarExpression<T>
    }

    /**
     * Function that visits an [InsertOrUpdateExpression].
     */
    public fun visitInsertOrUpdate(expr: InsertOrUpdateExpression): InsertOrUpdateExpression {
        val table = visitTable(expr.table)
        val assignments = visitExpressionList(expr.assignments)
        val conflictColumns = visitExpressionList(expr.conflictColumns)
        val updateAssignments = visitExpressionList(expr.updateAssignments)
        val returningColumns = visitExpressionList(expr.returningColumns)

        @Suppress("ComplexCondition")
        if (table === expr.table
            && assignments === expr.assignments
            && conflictColumns === expr.conflictColumns
            && updateAssignments === expr.updateAssignments
            && returningColumns === expr.returningColumns
        ) {
            return expr
        } else {
            return expr.copy(
                table = table,
                assignments = assignments,
                conflictColumns = conflictColumns,
                updateAssignments = updateAssignments,
                returningColumns = returningColumns
            )
        }
    }

    /**
     * Function that visits a [BulkInsertExpression].
     */
    public fun visitBulkInsert(expr: BulkInsertExpression): BulkInsertExpression {
        val table = visitTable(expr.table)
        val assignments = visitBulkInsertAssignments(expr.assignments)
        val conflictColumns = visitExpressionList(expr.conflictColumns)
        val updateAssignments = visitExpressionList(expr.updateAssignments)
        val returningColumns = visitExpressionList(expr.returningColumns)

        @Suppress("ComplexCondition")
        if (table === expr.table
            && assignments === expr.assignments
            && conflictColumns === expr.conflictColumns
            && updateAssignments === expr.updateAssignments
            && returningColumns === expr.returningColumns
        ) {
            return expr
        } else {
            return expr.copy(
                table = table,
                assignments = assignments,
                conflictColumns = conflictColumns,
                updateAssignments = updateAssignments,
                returningColumns = returningColumns
            )
        }
    }

    /**
     * Helper function for visiting insert assignments of [BulkInsertExpression].
     */
    public fun visitBulkInsertAssignments(
        assignments: List<List<ColumnAssignmentExpression<*>>>
    ): List<List<ColumnAssignmentExpression<*>>> {
        val result = ArrayList<List<ColumnAssignmentExpression<*>>>()
        var changed = false

        for (row in assignments) {
            val visited = visitExpressionList(row)
            result += visited

            if (visited !== row) {
                changed = true
            }
        }

        return if (changed) result else assignments
    }

    /**
     * Function that visits an [ILikeExpression].
     */
    public fun visitILike(expr: ILikeExpression): ILikeExpression {
        val left = visitScalar(expr.left)
        val right = visitScalar(expr.right)

        if (left === expr.left && right === expr.right) {
            return expr
        } else {
            return expr.copy(left = left, right = right)
        }
    }

    /**
     * Function that visits an [HStoreExpression].
     */
    public fun <T : Any> visitHStore(expr: HStoreExpression<T>): HStoreExpression<T> {
        val left = visitScalar(expr.left)
        val right = visitScalar(expr.right)

        if (left === expr.left && right === expr.right) {
            return expr
        } else {
            return expr.copy(left = left, right = right)
        }
    }

    /**
     * Function that visits a [CubeExpression].
     */
    public fun <T : Any> visitCube(expr: CubeExpression<T>): CubeExpression<T> {
        val left = visitScalar(expr.left)
        val right = visitScalar(expr.right)

        if (left === expr.left && right === expr.right) {
            return expr
        } else {
            return expr.copy(left = left, right = right)
        }
    }

    /**
     * Function that visits a [DefaultValueExpression].
     */
    public fun <T : Any> visitDefaultValue(expr: DefaultValueExpression<T>): DefaultValueExpression<T> {
        return expr
    }
}
