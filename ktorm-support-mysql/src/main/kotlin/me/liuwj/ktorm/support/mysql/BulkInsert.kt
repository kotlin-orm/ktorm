package me.liuwj.ktorm.support.mysql

import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.prepareStatement
import me.liuwj.ktorm.dsl.AssignmentsBuilder
import me.liuwj.ktorm.dsl.KtormDsl
import me.liuwj.ktorm.expression.ColumnAssignmentExpression
import me.liuwj.ktorm.expression.SqlExpression
import me.liuwj.ktorm.expression.TableExpression
import me.liuwj.ktorm.schema.Table

/**
 * 批量插入表达式
 *
 * @property table 要插入的表
 * @property assignments 赋值列表
 */
data class BulkInsertExpression(
    val table: TableExpression,
    val assignments: List<List<ColumnAssignmentExpression<*>>>,
    override val isLeafNode: Boolean = false
) : SqlExpression()

/**
 * 批量往表中插入数据，返回受影响的记录数
 *
 * Note：此方法与 batchInsert 不同，batchInsert 基于 JDBC 的 executeBatch 方法实现，此方法直接使用 MySQL 提供的
 * 批量插入语法实现，性能更好，如：insert into table (field1, field2) values (value1, value2), (value11, value22)...
 *
 * @see me.liuwj.ktorm.dsl.batchInsert
 * @see java.sql.Statement.executeBatch
 */
fun <T : Table<*>> T.bulkInsert(block: BulkInsertStatementBuilder<T>.() -> Unit): Int {
    val builder = BulkInsertStatementBuilder(this).apply(block)
    val expression = BulkInsertExpression(asExpression(), builder.allAssignments)

    expression.prepareStatement { statement ->
        val effects = statement.executeUpdate()

        val logger = Database.global.logger
        if (logger != null && logger.isDebugEnabled()) {
            logger.debug("Effects: $effects")
        }

        return effects
    }
}

@KtormDsl
class BulkInsertStatementBuilder<T : Table<*>>(internal val table: T) {
    internal val allAssignments = ArrayList<List<ColumnAssignmentExpression<*>>>()

    fun item(block: AssignmentsBuilder.(T) -> Unit) {
        val itemAssignments = ArrayList<ColumnAssignmentExpression<*>>()
        val builder = AssignmentsBuilder(itemAssignments)
        builder.block(table)

        if (allAssignments.isEmpty() || allAssignments[0].map { it.column.name } == itemAssignments.map { it.column.name }) {
            allAssignments += itemAssignments
        } else {
            throw IllegalArgumentException("Every item in a batch operation must be the same.")
        }
    }
}