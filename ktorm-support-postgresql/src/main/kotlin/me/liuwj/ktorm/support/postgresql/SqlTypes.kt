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

package me.liuwj.ktorm.support.postgresql

import me.liuwj.ktorm.schema.BaseTable
import me.liuwj.ktorm.schema.Column
import me.liuwj.ktorm.schema.SqlType
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

/**
 * Represent values of PostgreSQL `hstore` SQL type.
 */
typealias HStore = Map<String, String?>

/**
 * Represent values of PostgreSQL `text[]` SQL type.
 */
typealias TextArray = Array<String?>

/**
 * Define a column typed [HStoreSqlType].
 */
fun <E : Any> BaseTable<E>.hstore(name: String): Column<HStore> {
    return registerColumn(name, HStoreSqlType)
}

/**
 * [SqlType] implementation represents PostgreSQL `hstore` type.
 */
object HStoreSqlType : SqlType<HStore>(Types.OTHER, "hstore") {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: HStore) {
        ps.setObject(index, parameter)
    }

    @Suppress("UNCHECKED_CAST")
    override fun doGetResult(rs: ResultSet, index: Int): HStore? {
        return rs.getObject(index) as HStore?
    }
}

/**
 * Define a column typed [TextArraySqlType].
 */
fun <E : Any> BaseTable<E>.textArray(name: String): Column<TextArray> {
    return registerColumn(name, TextArraySqlType)
}

/**
 * [SqlType] implementation represents PostgreSQL `text[]` type.
 */
object TextArraySqlType : SqlType<TextArray>(Types.ARRAY, "text[]") {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: TextArray) {
        ps.setObject(index, parameter)
    }

    @Suppress("UNCHECKED_CAST")
    override fun doGetResult(rs: ResultSet, index: Int): TextArray? {
        val sqlArray = rs.getArray(index) ?: return null
        try {
            val objectArray = sqlArray.array as Array<Any?>?
            return objectArray?.map { it as String? }?.toTypedArray()
        } finally {
            sqlArray.free()
        }
    }
}

/**
 * Define a column typed of [PgEnumType].
 * !Note Enums are case sensitive and must match what is in the db
 *
 * @param <C> The Java enum type to use
 * @param name the column's name.
 * @return the registered column.
 */
inline fun <reified C : Enum<C>> BaseTable<*>.pgEnum(name: String): Column<C> {
    return registerColumn(name, PgEnumType(C::class.java))
}

/**
 * [SqlType] implementation represents PostgreSQL `enum` type.
 * @see <a href="https://www.postgresql.org/docs/current/datatype-enum.html">datatype-enum</a>
 */
class PgEnumType<C : Enum<C>>(private val enumClass: Class<C>) : SqlType<C>(Types.OTHER, enumClass.name) {
    private val valueOf = enumClass.getDeclaredMethod("valueOf", String::class.java)

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: C) {
        ps.setObject(index, parameter.name, Types.OTHER)
    }

    override fun doGetResult(rs: ResultSet, index: Int): C? {
        return rs.getString(index)?.takeIf { it.isNotBlank() }?.let { enumClass.cast(valueOf(null, it)) }
    }
}
