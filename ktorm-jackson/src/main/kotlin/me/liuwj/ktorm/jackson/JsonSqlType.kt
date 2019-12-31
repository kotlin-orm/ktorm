/*
 * Copyright 2018-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.liuwj.ktorm.jackson

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import me.liuwj.ktorm.schema.BaseTable
import me.liuwj.ktorm.schema.SqlType
import me.liuwj.ktorm.schema.TypeReference
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

/**
 * A shared [ObjectMapper] instance which is used as the default mapper of [json] SQL type.
 */
val sharedObjectMapper: ObjectMapper = ObjectMapper().registerModules(KtormModule(), KotlinModule(), JavaTimeModule())

/**
 * Define a column typed of [JsonSqlType].
 *
 * @param name the column's name.
 * @param typeRef the generic type infomation of this column, generally created by [me.liuwj.ktorm.schema.typeRef].
 * @param mapper the object mapper used to serialize column values to JSON strings and deserialize them.
 * @return the column registration that wraps the registered column.
 */
fun <E : Any, C : Any> BaseTable<E>.json(
    name: String,
    typeRef: TypeReference<C>,
    mapper: ObjectMapper = sharedObjectMapper
): BaseTable<E>.ColumnRegistration<C> {
    return registerColumn(name, JsonSqlType(mapper, mapper.constructType(typeRef.referencedType)))
}

/**
 * [SqlType] implementation that provides JSON data type support via Jackson framework.
 *
 * @property objectMapper the object mapper used to serialize column values to JSON strings and deserialize them.
 * @property javaType the generic type infomation represented as Jackson's [JavaType].
 */
class JsonSqlType<T : Any>(
    val objectMapper: ObjectMapper,
    val javaType: JavaType
) : SqlType<T>(Types.VARCHAR, "json") {

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
