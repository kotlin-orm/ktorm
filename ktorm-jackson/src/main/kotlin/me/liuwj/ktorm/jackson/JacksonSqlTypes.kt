package me.liuwj.ktorm.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import me.liuwj.ktorm.entity.Entity
import me.liuwj.ktorm.schema.SqlType
import me.liuwj.ktorm.schema.Table
import me.liuwj.ktorm.schema.TypeReference
import java.lang.reflect.Type
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

/**
 * [Table] 扩展函数，注册一个 json 数据类型的列，默认使用 [sharedObjectMapper] 进行序列化，也可自定义
 */
fun <E : Entity<E>, C : Any> Table<E>.json(
    name: String,
    typeReference: TypeReference<C>,
    objectMapper: ObjectMapper = sharedObjectMapper
): Table<E>.ColumnRegistration<C> {
    return registerColumn(name, JsonSqlType(typeReference.referencedType, objectMapper))
}

class JsonSqlType<T : Any>(type: Type, val objectMapper: ObjectMapper) : SqlType<T>(Types.VARCHAR, "json") {
    private val javaType = objectMapper.constructType(type)

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: T) {
        ps.setString(index, objectMapper.writeValueAsString(parameter))
    }

    override fun doGetResult(rs: ResultSet, index: Int): T? {
        val json = rs.getString(index)
        if (json.isNullOrBlank()) {
            return null
        } else {
            return objectMapper.readValue(json, javaType)
        }
    }
}
