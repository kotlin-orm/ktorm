package me.liuwj.ktorm.support.mysql

import me.liuwj.ktorm.database.prepareStatement
import me.liuwj.ktorm.dsl.AssignmentsBuilder
import me.liuwj.ktorm.dsl.KtOrmDsl
import me.liuwj.ktorm.expression.ColumnAssignmentExpression
import me.liuwj.ktorm.expression.SqlExpression
import me.liuwj.ktorm.expression.TableExpression
import me.liuwj.ktorm.schema.Table

/**
 * Upsert 表达式
 *
 * @property table 要插入的表
 * @property assignments 赋值列表
 * @property updateAssignments 当键冲突时，要更新的数据的赋值列表，on duplicate key update
 */
data class InsertOrUpdateExpression(
    val table: TableExpression,
    val assignments: List<ColumnAssignmentExpression<*>>,
    val updateAssignments: List<ColumnAssignmentExpression<*>> = emptyList(),
    override val isLeafNode: Boolean = false
) : SqlExpression()

/**
 * 往表中插入一条记录，键冲突时更新已有记录，返回受影响的记录数
 */
fun <T : Table<*>> T.insertOrUpdate(block: InsertOrUpdateStatementBuilder.(T) -> Unit): Int {
    val assignments = ArrayList<ColumnAssignmentExpression<*>>()
    val builder = InsertOrUpdateStatementBuilder(assignments).apply { block(this@insertOrUpdate) }

    val expression = InsertOrUpdateExpression(asExpression(), assignments, builder.updateAssignments)

    expression.prepareStatement { statement, logger ->
        return statement.executeUpdate().also { logger.debug("Effects: {}", it) }
    }
}

@KtOrmDsl
class InsertOrUpdateStatementBuilder(assignments: MutableList<ColumnAssignmentExpression<*>>) : AssignmentsBuilder(assignments) {
    internal val updateAssignments = ArrayList<ColumnAssignmentExpression<*>>()

    fun onDuplicateKey(block: AssignmentsBuilder.() -> Unit) {
        val assignments = ArrayList<ColumnAssignmentExpression<*>>()
        AssignmentsBuilder(assignments).apply(block)
        updateAssignments += assignments
    }
}