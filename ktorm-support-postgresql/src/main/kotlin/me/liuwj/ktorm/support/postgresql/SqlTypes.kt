package me.liuwj.ktorm.support.postgresql

import me.liuwj.ktorm.schema.BaseTable
import me.liuwj.ktorm.schema.SqlType
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

/**
 * Define a column typed [HstoreSqlType]
 */
fun <E : Any> BaseTable<E>.hstore(name: String): BaseTable<E>.ColumnRegistration<Map<String, String>> {
    return registerColumn(name, HstoreSqlType)
}

/**
 * [SqlType] implementation represents PostgreSQL `hstore` type.
 */
object HstoreSqlType : SqlType<Map<String, String>>(Types.OTHER, "hstore") {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Map<String, String>) {
        ps.setObject(index, parameter)
    }

    @Suppress("UNCHECKED_CAST")
    override fun doGetResult(rs: ResultSet, index: Int): Map<String, String>? {
        return rs.getObject(index) as Map<String, String>?
    }
}

/**
 * Define a column typed [TextArraySqlType]
 */
fun <E : Any> BaseTable<E>.textArray(name: String): BaseTable<E>.ColumnRegistration<Array<String>> {
    return registerColumn(name, TextArraySqlType)
}

/**
 * [SqlType] implementation represents PostgreSQL `text[]` type.
 */
object TextArraySqlType : SqlType<Array<String>>(Types.ARRAY, "text[]") {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Array<String>) {
        ps.setObject(index, parameter)
    }

    @Suppress("UNCHECKED_CAST")
    override fun doGetResult(rs: ResultSet, index: Int): Array<String>? {
        val sqlArray = rs.getArray(index)
        val objectArray = sqlArray.array as Array<Any>?
        return objectArray?.map { it as String }?.toTypedArray()
    }
}