package me.liuwj.ktorm.schema

import java.sql.PreparedStatement
import java.sql.ResultSet

/**
 * SQL 数据类型的统一抽象，封装从 [ResultSet] 获取数据、往 [PreparedStatement] 设置参数等通用操作，
 * 数据类型抽象为统一接口易于扩展，用户可以继承 [AbstractSqlType] 提供自己定制的数据类型
 */
interface SqlType<T : Any> {

    /**
     * 定义于 [java.sql.Types] 中的常量，用于标识该类型
     * @see [java.sql.Types]
     */
    val typeCode: Int

    /**
     * 该类型在其数据库中的字符串表示形式，如 int, bigint, varchar 等
     */
    val typeName: String

    /**
     * 往 [PreparedStatement] 中设置参数
     */
    fun setParameter(ps: PreparedStatement, index: Int, parameter: T?)

    /**
     * 使用 index 从 [ResultSet] 中获取数据
     */
    fun getResult(rs: ResultSet, index: Int): T?

    /**
     * 使用 label 从 [ResultSet] 中获取数据
     */
    fun getResult(rs: ResultSet, columnLabel: String): T?
}

/**
 * [SqlType] 的模版实现，提供了通用的 SqlType 的代码结构，一般来说，子类应该继承此类，而不是直接实现接口
 */
abstract class AbstractSqlType<T : Any>(override val typeCode: Int, override val typeName: String) : SqlType<T> {

    override fun setParameter(ps: PreparedStatement, index: Int, parameter: T?) {
        if (parameter == null) {
            ps.setNull(index, typeCode)
        } else {
            setNonNullParameter(ps, index, parameter)
        }
    }

    override fun getResult(rs: ResultSet, index: Int): T? {
        val result = getNullableResult(rs, index)
        return if (rs.wasNull()) null else result
    }

    override fun getResult(rs: ResultSet, columnLabel: String): T? {
        return getResult(rs, rs.findColumn(columnLabel))
    }

    /**
     * 往 [PreparedStatement] 中设置参数，该参数不可能为 null
     */
    protected abstract fun setNonNullParameter(ps: PreparedStatement, index: Int, parameter: T)

    /**
     * 使用 index 从 [ResultSet] 中获取参数，返回的结果可以为 null
     */
    protected abstract fun getNullableResult(rs: ResultSet, index: Int): T?
}