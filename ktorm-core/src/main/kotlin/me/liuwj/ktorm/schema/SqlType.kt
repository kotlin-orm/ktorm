package me.liuwj.ktorm.schema

import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.*

/**
 * SQL 数据类型的统一抽象，封装从 [ResultSet] 获取数据、往 [PreparedStatement] 设置参数等通用操作
 *
 * @property typeCode 定义于 [java.sql.Types] 中的常量，用于标识该类型
 * @property typeName 该类型在其数据库中的字符串表示形式，如 int, bigint, varchar 等
 */
abstract class SqlType<T : Any>(val typeCode: Int, val typeName: String) {

    /**
     * 往 [PreparedStatement] 中设置参数，该参数不可能为 null
     */
    protected abstract fun doSetParameter(ps: PreparedStatement, index: Int, parameter: T)

    /**
     * 使用 index 从 [ResultSet] 中获取参数，返回的结果可以为 null
     */
    protected abstract fun doGetResult(rs: ResultSet, index: Int): T?

    /**
     * 往 [PreparedStatement] 中设置参数
     */
    open fun setParameter(ps: PreparedStatement, index: Int, parameter: T?) {
        if (parameter == null) {
            ps.setNull(index, typeCode)
        } else {
            doSetParameter(ps, index, parameter)
        }
    }

    /**
     * 使用 index 从 [ResultSet] 中获取数据
     */
    open fun getResult(rs: ResultSet, index: Int): T? {
        val result = doGetResult(rs, index)
        return if (rs.wasNull()) null else result
    }

    /**
     * 使用 label 从 [ResultSet] 中获取数据
     */
    open fun getResult(rs: ResultSet, columnLabel: String): T? {
        return getResult(rs, rs.findColumn(columnLabel))
    }

    override fun equals(other: Any?): Boolean {
        return other is SqlType<*> && this.typeCode == other.typeCode && this.typeName == other.typeName
    }

    override fun hashCode(): Int {
        return Objects.hash(typeCode, typeName)
    }
}
