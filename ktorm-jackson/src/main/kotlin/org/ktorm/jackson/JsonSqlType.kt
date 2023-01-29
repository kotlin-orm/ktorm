/*
 * Copyright 2018-2023 the original author or authors.
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

package org.ktorm.jackson

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.ktorm.schema.*
import java.lang.reflect.InvocationTargetException
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

/**
 * A shared [ObjectMapper] instance which is used as the default mapper of [json] SQL type.
 */
public val sharedObjectMapper: ObjectMapper = ObjectMapper()
    .registerModule(KtormModule())
    .registerModule(KotlinModule())
    .registerModule(JavaTimeModule())

/**
 * Define a column typed of [JsonSqlType].
 *
 * @param name the column's name.
 * @param mapper the object mapper used to serialize column values to JSON strings and deserialize them.
 * @return the registered column.
 */
public inline fun <reified C : Any> BaseTable<*>.json(
    name: String,
    mapper: ObjectMapper = sharedObjectMapper
): Column<C> {
    return registerColumn(name, JsonSqlType(mapper, mapper.constructType(typeOf<C>())))
}

/**
 * [SqlType] implementation that provides JSON data type support via Jackson framework.
 *
 * @property objectMapper the object mapper used to serialize column values to JSON strings and deserialize them.
 * @property javaType the generic type information represented as Jackson's [JavaType].
 */
public class JsonSqlType<T : Any>(
    public val objectMapper: ObjectMapper,
    public val javaType: JavaType
) : SqlType<T>(Types.OTHER, "json") {
    // Access postgresql API by reflection, because it is not a JDK 9 module,
    // we are not able to require it in module-info.java.
    private val pgStatementClass = loadClass("org.postgresql.PGStatement")
    private val pgObjectClass = loadClass("org.postgresql.util.PGobject")
    private val pgObjectConstructor = pgObjectClass?.getDeclaredConstructor()
    private val setTypeMethod = pgObjectClass?.getMethod("setType", String::class.java)
    private val setValueMethod = pgObjectClass?.getMethod("setValue", String::class.java)

    private fun loadClass(name: String): Class<*>? {
        try {
            return Class.forName(name)
        } catch (_: ClassNotFoundException) {
            return null
        }
    }

    override fun setParameter(ps: PreparedStatement, index: Int, parameter: T?) {
        if (parameter != null) {
            doSetParameter(ps, index, parameter)
        } else {
            if (pgStatementClass != null && ps.isWrapperFor(pgStatementClass)) {
                ps.setNull(index, Types.OTHER)
            } else {
                ps.setNull(index, Types.VARCHAR)
            }
        }
    }

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: T) {
        if (pgStatementClass == null || !ps.isWrapperFor(pgStatementClass)) {
            ps.setString(index, objectMapper.writeValueAsString(parameter))
        } else {
            @Suppress("SwallowedException")
            try {
                val obj = pgObjectConstructor!!.newInstance()
                setTypeMethod!!.invoke(obj, "json")
                setValueMethod!!.invoke(obj, objectMapper.writeValueAsString(parameter))
                ps.setObject(index, obj)
            } catch (e: InvocationTargetException) {
                throw e.targetException
            }
        }
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
