/*
 * Copyright 2018-2019 the original author or authors.
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

package me.liuwj.ktorm.expression

import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.DialectFeatureNotSupportedException

/**
 * Subclass of [SqlExpressionVisitor], visiting SQL expression trees using visitor pattern. After the visit completes,
 * the executable SQL string will be generated in the [sql] property with its execution parameters in [parameters].
 *
 * @property database the current database object used to obtain metadata such as identifier quote string.
 * @property beautifySql mark if we should output beautiful SQL strings with line-wrapping and indentation.
 * @property indentSize the indent size.
 * @property sql return the executable SQL string after the visit completes.
 * @property parameters return the SQL's execution parameters after the visit completes.
 */
@Suppress("VariableNaming")
abstract class SqlFormatter(
    val database: Database,
    val beautifySql: Boolean,
    val indentSize: Int
) : SqlExpressionVisitor() {

    protected var _depth = 0
    protected val _builder = StringBuilder()
    protected val _parameters = ArrayList<ArgumentExpression<*>>()

    val sql: String get() = _builder.toString()
    val parameters: List<ArgumentExpression<*>> get() = _parameters

    protected enum class Indentation {
        INNER, OUTER, SAME
    }

    protected fun write(value: String) {
        _builder.append(value)
    }

    protected fun removeLastBlank() {
        val lastIndex = _builder.lastIndex
        if (_builder[lastIndex] == ' ') {
            _builder.deleteCharAt(lastIndex)
        }
    }

    protected fun newLine(indent: Indentation) {
        when (indent) {
            Indentation.INNER -> _depth++
            Indentation.OUTER -> _depth--
            else -> { }
        }

        check(_depth >= 0) { "Incorrect indent depth: $_depth" }

        if (beautifySql) {
            _builder.appendln()
            _builder.append(" ".repeat(_depth * indentSize))
        }
    }

    protected val String.quoted: String get() {
        if (this.toUpperCase() in database.keywords || !this.isIdentifier) {
            return "${database.identifierQuoteString}${this}${database.identifierQuoteString}".trim()
        } else {
            return this
        }
    }

    protected val String.isIdentifier: Boolean get() {
        if (this.isEmpty()) {
            return false
        } else {
            return this[0].isIdentifierStart && all { it.isIdentifierStart || it in '0'..'9' }
        }
    }

    protected val Char.isIdentifierStart: Boolean get() {
        if (this in 'a'..'z') {
            return true
        }
        if (this in 'A'..'Z') {
            return true
        }
        if (this == '_') {
            return true
        }
        if (this in database.extraNameCharacters) {
            return true
        }
        return false
    }

    protected open val SqlExpression.removeBrackets: Boolean get() {
        return isLeafNode || this is FunctionExpression<*> || this is AggregateExpression<*> || this is ExistsExpression
    }

    override fun visit(expr: SqlExpression): SqlExpression {
        val result = super.visit(expr)
        check(result === expr) { "SqlFormatter cannot modify the expression trees." }
        return result
    }

    override fun <T : SqlExpression> visitExpressionList(original: List<T>): List<T> {
        for ((i, expr) in original.withIndex()) {
            if (i > 0) {
                removeLastBlank()
                write(", ")
            }
            visit(expr)
        }
        return original
    }

    override fun <T : Any> visitArgument(expr: ArgumentExpression<T>): ArgumentExpression<T> {
        write("? ")
        _parameters += expr
        return expr
    }

    override fun <T : Any> visitUnary(expr: UnaryExpression<T>): UnaryExpression<T> {
        if (expr.type == UnaryExpressionType.IS_NULL || expr.type == UnaryExpressionType.IS_NOT_NULL) {
            if (expr.operand.removeBrackets) {
                visit(expr.operand)
            } else {
                write("(")
                visit(expr.operand)
                removeLastBlank()
                write(") ")
            }

            write("${expr.type} ")
        } else {
            write("${expr.type} ")

            if (expr.operand.removeBrackets) {
                visit(expr.operand)
            } else {
                write("(")
                visit(expr.operand)
                removeLastBlank()
                write(") ")
            }
        }

        return expr
    }

    override fun <T : Any> visitBinary(expr: BinaryExpression<T>): BinaryExpression<T> {
        if (expr.left.removeBrackets) {
            visit(expr.left)
        } else {
            write("(")
            visit(expr.left)
            removeLastBlank()
            write(") ")
        }

        write("${expr.type} ")

        if (expr.right.removeBrackets) {
            visit(expr.right)
        } else {
            write("(")
            visit(expr.right)
            removeLastBlank()
            write(") ")
        }

        return expr
    }

    override fun visitTable(expr: TableExpression): TableExpression {
        write("${expr.name.quoted} ")
        expr.tableAlias?.let { write("${it.quoted} ") }
        return expr
    }

    override fun <T : Any> visitAggregate(expr: AggregateExpression<T>): AggregateExpression<T> {
        write("${expr.type}(")
        if (expr.isDistinct) {
            write("distinct ")
        }
        expr.argument?.let { visit(it) } ?: write("*")
        removeLastBlank()
        write(") ")
        return expr
    }

    override fun <T : Any> visitColumn(expr: ColumnExpression<T>): ColumnExpression<T> {
        expr.tableAlias?.let { write("${it.quoted}.") }
        write("${expr.name.quoted} ")
        return expr
    }

    override fun visitColumnDeclaring(expr: ColumnDeclaringExpression): ColumnDeclaringExpression {
        /*if (expr.expression is SelectExpression) {
            write("(")
            newLine(Indentation.INNER)
            visit(expr.expression)
            newLine(Indentation.OUTER)
            write(") ")
        } else {
            visit(expr.expression)
        }*/

        visit(expr.expression)

        val column = expr.expression as? ColumnExpression<*>
        val hasDeclaredName = expr.declaredName != null && expr.declaredName.isNotBlank()

        if (hasDeclaredName && (column == null || column.name != expr.declaredName)) {
            write("as ${expr.declaredName!!.quoted} ")
        }

        return expr
    }

    override fun visitOrderBy(expr: OrderByExpression): OrderByExpression {
        visit(expr.expression)
        if (expr.orderType == OrderType.DESCENDING) {
            write("desc ")
        }
        return expr
    }

    override fun visitSelect(expr: SelectExpression): SelectExpression {
        write("select ")
        if (expr.isDistinct) {
            write("distinct ")
        }

        if (expr.columns.isNotEmpty()) {
            visitColumnDeclaringList(expr.columns)
        } else {
            write("* ")
        }

        newLine(Indentation.SAME)
        write("from ")
        visitQuerySource(expr.from)

        if (expr.where != null) {
            newLine(Indentation.SAME)
            write("where ")
            visit(expr.where)
        }
        if (expr.groupBy.isNotEmpty()) {
            newLine(Indentation.SAME)
            write("group by ")
            visitGroupByList(expr.groupBy)
        }
        if (expr.having != null) {
            newLine(Indentation.SAME)
            write("having ")
            visit(expr.having)
        }
        if (expr.orderBy.isNotEmpty()) {
            newLine(Indentation.SAME)
            write("order by ")
            visitOrderByList(expr.orderBy)
        }
        if (expr.offset != null || expr.limit != null) {
            writePagination(expr)
        }
        return expr
    }

    override fun visitQuerySource(expr: QuerySourceExpression): QuerySourceExpression {
        when (expr) {
            is TableExpression -> {
                visitTable(expr)
            }
            is JoinExpression -> {
                visitJoin(expr)
            }
            is QueryExpression -> {
                write("(")
                newLine(Indentation.INNER)
                visitQuery(expr)
                removeLastBlank()
                newLine(Indentation.OUTER)
                write(") ")
                expr.tableAlias?.let { write("${it.quoted} ") }
            }
            else -> {
                visitUnknown(expr)
            }
        }

        return expr
    }

    override fun visitUnion(expr: UnionExpression): UnionExpression {
        when (expr.left) {
            is SelectExpression -> visitQuerySource(expr.left)
            is UnionExpression -> visitUnion(expr.left)
        }

        if (expr.isUnionAll) {
            write("union all ")
        } else {
            write("union ")
        }

        when (expr.right) {
            is SelectExpression -> visitQuerySource(expr.right)
            is UnionExpression -> visitUnion(expr.right)
        }

        if (expr.orderBy.isNotEmpty()) {
            newLine(Indentation.SAME)
            write("order by ")
            visitOrderByList(expr.orderBy)
        }
        if (expr.offset != null || expr.limit != null) {
            writePagination(expr)
        }
        return expr
    }

    protected open fun writePagination(expr: QueryExpression) {
        throw DialectFeatureNotSupportedException("Pagination is not supported in Standard SQL.")
    }

    override fun visitJoin(expr: JoinExpression): JoinExpression {
        visitQuerySource(expr.left)
        newLine(Indentation.SAME)
        write("${expr.type} ")
        visitQuerySource(expr.right)

        if (expr.condition != null) {
            write("on ")
            visit(expr.condition)
        }

        return expr
    }

    override fun <T : Any> visitInList(expr: InListExpression<T>): InListExpression<T> {
        visit(expr.left)

        if (expr.notInList) {
            write("not in ")
        } else {
            write("in ")
        }

        if (expr.query != null) {
            visitQuerySource(expr.query)
        }
        if (expr.values != null) {
            write("(")
            visitExpressionList(expr.values)
            removeLastBlank()
            write(") ")
        }
        return expr
    }

    override fun visitExists(expr: ExistsExpression): ExistsExpression {
        if (expr.notExists) {
            write("not exists ")
        } else {
            write("exists ")
        }

        visitQuerySource(expr.query)

        return expr
    }

    override fun <T : Any> visitBetween(expr: BetweenExpression<T>): BetweenExpression<T> {
        visit(expr.expression)

        if (expr.notBetween) {
            write("not between ")
        } else {
            write("between ")
        }

        visit(expr.lower)
        write("and ")
        visit(expr.upper)
        return expr
    }

    override fun <T : Any> visitFunction(expr: FunctionExpression<T>): FunctionExpression<T> {
        write("${expr.functionName}(")
        visitExpressionList(expr.arguments)
        removeLastBlank()
        write(") ")
        return expr
    }

    override fun visitColumnAssignments(
        original: List<ColumnAssignmentExpression<*>>
    ): List<ColumnAssignmentExpression<*>> {
        for ((i, assignment) in original.withIndex()) {
            if (i > 0) {
                removeLastBlank()
                write(", ")
            }
            visitColumn(assignment.column)
            write("= ")
            visit(assignment.expression)
        }

        return original
    }

    override fun visitInsert(expr: InsertExpression): InsertExpression {
        write("insert into ${expr.table.name.quoted} (")
        for ((i, assignment) in expr.assignments.withIndex()) {
            if (i > 0) write(", ")
            write(assignment.column.name.quoted)
        }
        write(") values (")
        visitExpressionList(expr.assignments.map { it.expression as ArgumentExpression })
        removeLastBlank()
        write(") ")
        return expr
    }

    override fun visitInsertFromQuery(expr: InsertFromQueryExpression): InsertFromQueryExpression {
        write("insert into ${expr.table.name.quoted} (")
        for ((i, column) in expr.columns.withIndex()) {
            if (i > 0) write(", ")
            write(column.name.quoted)
        }
        write(") ")

        visitQuery(expr.query)

        return expr
    }

    override fun visitUpdate(expr: UpdateExpression): UpdateExpression {
        write("update ")
        visitTable(expr.table)
        write("set ")

        visitColumnAssignments(expr.assignments)

        if (expr.where != null) {
            write("where ")
            visit(expr.where)
        }

        return expr
    }

    override fun visitDelete(expr: DeleteExpression): DeleteExpression {
        write("delete from ")
        visit(expr.table)

        if (expr.where != null) {
            write("where ")
            visit(expr.where)
        }

        return expr
    }

    override fun visitUnknown(expr: SqlExpression): SqlExpression {
        throw DialectFeatureNotSupportedException("Unsupported expression type: ${expr.javaClass}")
    }
}
