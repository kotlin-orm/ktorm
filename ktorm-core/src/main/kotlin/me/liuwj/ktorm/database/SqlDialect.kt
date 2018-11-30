package me.liuwj.ktorm.database

import me.liuwj.ktorm.expression.SqlFormatter

/**
 * SQL dialect interface
 */
interface SqlDialect {

    /**
     * 创建一个 [SqlFormatter] 对象，将 SQL 表达式格式化为可直接执行的 SQL
     */
    fun createSqlFormatter(database: Database, beautifySql: Boolean, indentSize: Int): SqlFormatter
}

/**
 * Dialect for standard SQL
 */
object StandardDialect : SqlDialect {

    override fun createSqlFormatter(database: Database, beautifySql: Boolean, indentSize: Int): SqlFormatter {
        return object : SqlFormatter(database, beautifySql, indentSize) { }
    }
}