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
 * Define a column typed [HstoreSqlType].
 */
fun <E : Any> BaseTable<E>.hstore(name: String): Column<Hstore> {
    return registerColumn(name, HstoreSqlType)
}

/**
 * [SqlType] implementation represents PostgreSQL `hstore` type.
 */
object HstoreSqlType : SqlType<Hstore>(Types.OTHER, "hstore") {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Hstore) {
        ps.setObject(index, parameter)
    }

    @Suppress("UNCHECKED_CAST")
    override fun doGetResult(rs: ResultSet, index: Int): Hstore? {
        return rs.getObject(index) as Hstore?
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
        val sqlArray = rs.getArray(index)
        val objectArray = sqlArray.array as Array<Any>?
        return objectArray?.map { it as String? }?.toTypedArray()
    }
}
