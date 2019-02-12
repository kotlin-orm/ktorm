package me.liuwj.ktorm.expression

/**
 * 使用 Visitor 设计模式对 SQL 语法树进行遍历
 *
 * 本类提供一个通用的 [visit] 方法来根据类型将不同的表达式分发到不同的 visit* 方法中，对于未知类型的表达式，则调用 [visitUnknown] 方法，
 * 子类可以选择性地覆盖具体的 visit* 方法，以在遍历过程中选择自己感兴趣的节点
 *
 * 如果要在遍历过程中修改表达式，则重写某个 visit* 方法，在其中返回修改后的新节点。本类中的所有 visit* 方法在遍历其子节点之后，都会检查子节点
 * 是否发生改变，如果子节点发生改变，则返回一个新的父节点表达式，新表达式中包含了新的子节点，最终 [visit] 方法返回的表达式就是我们遍历修改后的结果
 *
 * Created by vince on May 18, 2018.
 */
abstract class SqlExpressionVisitor {

    open fun visit(expr: SqlExpression): SqlExpression {
        return when (expr) {
            is ScalarExpression<*> -> visitScalar(expr)
            is QueryExpression -> visitQuery(expr)
            is QuerySourceExpression -> visitQuerySource(expr)
            is ColumnDeclaringExpression -> visitColumnDeclaring(expr)
            is OrderByExpression -> visitOrderBy(expr)
            is ColumnAssignmentExpression<*> -> visitColumnAssignment(expr)
            is InsertExpression -> visitInsert(expr)
            is InsertFromQueryExpression -> visitInsertFromQuery(expr)
            is UpdateExpression -> visitUpdate(expr)
            is DeleteExpression -> visitDelete(expr)
            else -> visitUnknown(expr)
        }
    }

    protected open fun <T : Any> visitScalar(expr: ScalarExpression<T>): ScalarExpression<T> {
        val result = when (expr) {
            is CastingExpression -> visitCasting(expr)
            is UnaryExpression -> visitUnary(expr)
            is BinaryExpression -> visitBinary(expr)
            is ColumnExpression -> visitColumn(expr)
            is InListExpression<*> -> visitInList(expr)
            is AggregateExpression -> visitAggregate(expr)
            is BetweenExpression<*> -> visitBetween(expr)
            is ArgumentExpression -> visitArgument(expr)
            is FunctionExpression -> visitFunction(expr)
            else -> visitUnknown(expr)
        }

        @Suppress("UNCHECKED_CAST")
        return result as ScalarExpression<T>
    }

    protected open fun visitQuerySource(expr: QuerySourceExpression): QuerySourceExpression {
        return when (expr) {
            is TableExpression -> visitTable(expr)
            is JoinExpression -> visitJoin(expr)
            is QueryExpression -> visitQuery(expr)
            else -> visitUnknown(expr) as QuerySourceExpression
        }
    }

    protected open fun visitQuery(expr: QueryExpression): QueryExpression {
        return when (expr) {
            is SelectExpression -> visitSelect(expr)
            is UnionExpression -> visitUnion(expr)
        }
    }

    protected open fun <T : Any> visitCasting(expr: CastingExpression<T>): CastingExpression<T> {
        val expression = visit(expr.expression)

        if (expression === expr.expression) {
            return expr
        } else {
            return expr.copy(expression = expression)
        }
    }

    protected open fun <T : SqlExpression> visitExpressionList(original: List<T>): List<T> {
        val result = ArrayList<T>()
        var changed = false

        for (expr in original) {
            @Suppress("UNCHECKED_CAST")
            val visited = visit(expr) as T
            result += visited

            if (visited !== expr) {
                changed = true
            }
        }

        return if (changed) result else original
    }

    protected open fun <T : Any> visitUnary(expr: UnaryExpression<T>): UnaryExpression<T> {
        val operand = visitScalar(expr.operand)

        if (operand === expr.operand) {
            return expr
        } else {
            return expr.copy(operand = operand)
        }
    }

    protected open fun <T : Any> visitBinary(expr: BinaryExpression<T>): BinaryExpression<T> {
        val left = visitScalar(expr.left)
        val right = visitScalar(expr.right)

        if (left === expr.left && right === expr.right) {
            return expr
        } else {
            return expr.copy(left = left, right = right)
        }
    }

    protected open fun visitTable(expr: TableExpression): TableExpression {
        return expr
    }

    protected open fun <T : Any> visitColumn(expr: ColumnExpression<T>): ColumnExpression<T> {
        return expr
    }

    protected open fun visitColumnDeclaring(expr: ColumnDeclaringExpression): ColumnDeclaringExpression {
        val expression = visitScalar(expr.expression)

        if (expression === expr.expression) {
            return expr
        } else {
            return expr.copy(expression = expression)
        }
    }

    protected open fun visitOrderBy(expr: OrderByExpression): OrderByExpression {
        val expression = visitScalar(expr.expression)

        if (expression === expr.expression) {
            return expr
        } else {
            return expr.copy(expression = expression)
        }
    }

    protected open fun visitColumnDeclaringList(original: List<ColumnDeclaringExpression>): List<ColumnDeclaringExpression> {
        return visitExpressionList(original)
    }

    protected open fun visitOrderByList(original: List<OrderByExpression>): List<OrderByExpression> {
        return visitExpressionList(original)
    }

    protected open fun visitGroupByList(original: List<ScalarExpression<*>>): List<ScalarExpression<*>> {
        return visitExpressionList(original)
    }

    protected open fun visitSelect(expr: SelectExpression): SelectExpression {
        val columns = visitColumnDeclaringList(expr.columns)
        val from = visitQuerySource(expr.from)
        val where = expr.where?.let { visitScalar(it) }
        val groupBy = visitGroupByList(expr.groupBy)
        val having = expr.having?.let { visitScalar(it) }
        val orderBy = visitOrderByList(expr.orderBy)

        if (columns === expr.columns
            && from === expr.from
            && where === expr.where
            && orderBy === expr.orderBy
            && groupBy === expr.groupBy
            && having === expr.having) {
            return expr
        } else {
            return expr.copy(columns = columns, from = from, where = where, groupBy = groupBy, having = having, orderBy = orderBy)
        }
    }

    protected open fun visitUnion(expr: UnionExpression): UnionExpression {
        val left = visitQuery(expr.left)
        val right = visitQuery(expr.right)
        val orderBy = visitOrderByList(expr.orderBy)

        if (left === expr.left && right === expr.right && orderBy === expr.orderBy) {
            return expr
        } else {
            return expr.copy(left = left, right = right, orderBy = orderBy)
        }
    }

    protected open fun visitJoin(expr: JoinExpression): JoinExpression {
        val left = visitQuerySource(expr.left)
        val right = visitQuerySource(expr.right)
        val condition = expr.condition?.let { visitScalar(it) }

        if (left === expr.left && right === expr.right && condition === expr.condition) {
            return expr
        } else {
            return expr.copy(left = left, right = right, condition = condition)
        }
    }

    protected open fun <T : Any> visitInList(expr: InListExpression<T>): InListExpression<T> {
        val left = visitScalar(expr.left)
        val query = expr.query?.let { visitQuery(it) }
        val values = expr.values?.let { visitExpressionList(it) }

        if (left === expr.left && query === expr.query && values === expr.values) {
            return expr
        } else {
            return expr.copy(left = left, query = query, values = values)
        }
    }

    protected open fun <T : Any> visitAggregate(expr: AggregateExpression<T>): AggregateExpression<T> {
        val argument = expr.argument?.let { visitScalar(it) }

        if (argument === expr.argument) {
            return expr
        } else {
            return expr.copy(argument = argument)
        }
    }

    protected open fun <T : Any> visitBetween(expr: BetweenExpression<T>): BetweenExpression<T> {
        val expression = visitScalar(expr.expression)
        val lower = visitScalar(expr.lower)
        val upper = visitScalar(expr.upper)

        if (expression === expr.expression && lower === expr.lower && upper === expr.upper) {
            return expr
        } else {
            return expr.copy(expression = expression, lower = lower, upper = upper)
        }
    }

    protected open fun <T : Any> visitArgument(expr: ArgumentExpression<T>): ArgumentExpression<T> {
        return expr
    }

    protected open fun <T : Any> visitFunction(expr: FunctionExpression<T>): FunctionExpression<T> {
        val arguments = visitExpressionList(expr.arguments)

        if (arguments === expr.arguments) {
            return expr
        } else {
            return expr.copy(arguments = arguments)
        }
    }

    protected open fun <T : Any> visitColumnAssignment(expr: ColumnAssignmentExpression<T>): ColumnAssignmentExpression<T> {
        val column = visitColumn(expr.column)
        val expression = visitScalar(expr.expression)

        if (column === expr.column && expression === expr.expression) {
            return expr
        } else {
            return expr.copy(column, expression)
        }
    }

    protected open fun visitColumnAssignments(original: List<ColumnAssignmentExpression<*>>): List<ColumnAssignmentExpression<*>> {
        return visitExpressionList(original)
    }

    protected open fun visitInsert(expr: InsertExpression): InsertExpression {
        val table = visitTable(expr.table)
        val assignments = visitColumnAssignments(expr.assignments)

        if (table === expr.table && assignments === expr.assignments) {
            return expr
        } else {
            return expr.copy(table = table, assignments = assignments)
        }
    }

    protected open fun visitInsertFromQuery(expr: InsertFromQueryExpression): InsertFromQueryExpression {
        val table = visitTable(expr.table)
        val columns = visitExpressionList(expr.columns)
        val query = visitQuery(expr.query)

        if (table === expr.table && columns === expr.columns && query === expr.query) {
            return expr
        } else {
            return expr.copy(table = table, columns = columns, query = query)
        }
    }

    protected open fun visitUpdate(expr: UpdateExpression): UpdateExpression {
        val table = visitTable(expr.table)
        val assignments = visitColumnAssignments(expr.assignments)
        val where = expr.where?.let { visitScalar(it) }

        if (table === expr.table && assignments === expr.assignments && where === expr.where) {
            return expr
        } else {
            return expr.copy(table = table, assignments = assignments, where = where)
        }
    }

    protected open fun visitDelete(expr: DeleteExpression): DeleteExpression {
        val table = visitTable(expr.table)
        val where = expr.where?.let { visitScalar(it) }

        if (table === expr.table && where === expr.where) {
            return expr
        } else {
            return expr.copy(table = table, where = where)
        }
    }

    protected open fun visitUnknown(expr: SqlExpression): SqlExpression {
        return expr
    }
}