package me.liuwj.ktorm.entity

import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.prepareStatement
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.expression.*
import me.liuwj.ktorm.schema.ColumnDeclaring
import me.liuwj.ktorm.schema.IntSqlType
import me.liuwj.ktorm.schema.Table

/**
 * 如果表中的所有行都符合指定条件，返回 true，否则 false
 */
fun <T : Table<*>> T.all(block: (T) -> ScalarExpression<Boolean>): Boolean {
    return none { !block(this) }
}

/**
 * 如果表中有数据，返回 true，否则 false
 */
fun Table<*>.any(): Boolean {
    return count() > 0
}

/**
 * 如果表中存在任何一条记录满足指定条件，返回 true，否则 false
 */
fun <T : Table<*>> T.any(block: (T) -> ScalarExpression<Boolean>): Boolean {
    return count(block) > 0
}

/**
 * 如果表中没有数据，返回 true，否则 false
 */
fun Table<*>.none(): Boolean {
    return count() == 0
}

/**
 * 如果表中所有记录都不满足指定条件，返回 true，否则 false
 */
fun <T : Table<*>> T.none(block: (T) -> ScalarExpression<Boolean>): Boolean {
    return count(block) == 0
}

/**
 * 返回表中的记录数
 */
fun Table<*>.count(): Int {
    return doCount(null)
}

/**
 * 返回表中满足指定条件的记录数
 */
fun <T : Table<*>> T.count(block: (T) -> ScalarExpression<Boolean>): Int {
    return doCount(block)
}

private fun <T : Table<*>> T.doCount(block: ((T) -> ScalarExpression<Boolean>)?): Int {
    val expression = SelectExpression(
        columns = listOf(
            ColumnDeclaringExpression(
                expression = AggregateExpression(
                    type = AggregateType.COUNT,
                    argument = null,
                    isDistinct = false,
                    sqlType = IntSqlType
                )
            )
        ),
        from = this.asExpression(),
        where = block?.invoke(this)
    )

    expression.prepareStatement { statement, logger ->
        statement.executeQuery().use { rs ->
            if (rs.next()) {
                return rs.getInt(1).also { logger.debug("Count: {}", it) }
            } else {
                val (sql, _) = Database.global.formatExpression(expression, beautifySql = true)
                throw IllegalStateException("No result return for sql: $sql")
            }
        }
    }
}

/**
 * 返回表中指定字段的和，若表中没有数据，返回 null
 */
fun <T : Table<*>, C : Number> T.sumBy(block: (T) -> ColumnDeclaring<C>): C? {
    return doAggregation(sum(block(this)))
}

/**
 * 返回表中指定字段的最大值，若表中没有数据，返回 null
 */
fun <T : Table<*>, C : Number> T.maxBy(block: (T) -> ColumnDeclaring<C>): C? {
    return doAggregation(max(block(this)))
}

/**
 * 返回表中指定字段的最小值，若表中没有数据，返回 null
 */
fun <T : Table<*>, C : Number> T.minBy(block: (T) -> ColumnDeclaring<C>): C? {
    return doAggregation(min(block(this)))
}

/**
 * 返回表中指定字段的平均值，若表中没有数据，返回 null
 */
fun <T : Table<*>> T.avgBy(block: (T) -> ColumnDeclaring<out Number>): Double? {
    return doAggregation(avg(block(this)))
}

private fun <R : Number> Table<*>.doAggregation(aggregation: AggregateExpression<R>): R? {
    val expression = SelectExpression(
        columns = kotlin.collections.listOf(
            ColumnDeclaringExpression(
                expression = aggregation.asExpression()
            )
        ),
        from = this.asExpression()
    )

    expression.prepareStatement { statement, logger ->
        statement.executeQuery().use { rs ->
            if (rs.next()) {
                val result = aggregation.sqlType.getResult(rs, 1)

                if (logger.isDebugEnabled) {
                    logger.debug("{}: {}", aggregation.type.toString().capitalize(), result)
                }

                return result
            } else {
                return null
            }
        }
    }
}