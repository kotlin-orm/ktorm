package me.liuwj.ktorm.jackson

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import me.liuwj.ktorm.entity.Entity
import me.liuwj.ktorm.schema.SqlType
import me.liuwj.ktorm.schema.Table
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import kotlin.reflect.KClass

/**
 * [Table] 扩展函数，注册一个 json 数据类型的列，默认使用 [sharedObjectMapper] 进行序列化，也可自定义
 */
fun <E : Entity<E>, C : Any> Table<E>.json(
    name: String,
    type: KClass<C>,
    objectMapper: ObjectMapper = sharedObjectMapper
): Table<E>.ColumnRegistration<C> {
    return registerColumn(name, JsonSqlType(type, objectMapper))
}

class JsonSqlType<T : Any>(
    val type: KClass<T>,
    val objectMapper: ObjectMapper
) : SqlType<T>(Types.VARCHAR, "json") {

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: T) {
        ps.setString(index, objectMapper.writeValueAsString(parameter))
    }

    override fun doGetResult(rs: ResultSet, index: Int): T? {
        val json = rs.getString(index)
        if (json.isNullOrBlank()) {
            return null
        } else {
            return objectMapper.readValue(json, type.java)
        }
    }
}

/**
 * [Table] 扩展函数，注册一个 json 列表数据类型的列，默认使用 [sharedObjectMapper] 进行序列化，也可自定义
 */
fun <E : Entity<E>, C : Any> Table<E>.listJson(
    name: String,
    elementType: KClass<C>,
    objectMapper: ObjectMapper = sharedObjectMapper
): Table<E>.ColumnRegistration<List<C>> {
    return registerColumn(name, ListJsonSqlType(elementType, objectMapper))
}

class ListJsonSqlType<T : Any>(
    val elementType: KClass<T>,
    val objectMapper: ObjectMapper
) : SqlType<List<T>>(Types.VARCHAR, "json") {

    val collectionType: JavaType = objectMapper.typeFactory.constructCollectionType(List::class.java, elementType.java)

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: List<T>) {
        ps.setString(index, objectMapper.writeValueAsString(parameter))
    }

    override fun doGetResult(rs: ResultSet, index: Int): List<T>? {
        val json = rs.getString(index)
        if (json.isNullOrBlank() || json == "[]") {
            return emptyList()
        } else {
            return objectMapper.readValue(json, collectionType)
        }
    }
}
