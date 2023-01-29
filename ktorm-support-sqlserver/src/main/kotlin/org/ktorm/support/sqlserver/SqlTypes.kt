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

@file:Suppress("MatchingDeclarationName")

package org.ktorm.support.sqlserver

import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import org.ktorm.schema.SqlType
import java.lang.reflect.InvocationTargetException
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.OffsetDateTime

/**
 * Type code constant copied from microsoft.sql.Types.DATETIMEOFFSET.
 */
private const val TYPE_CODE_DATETIMEOFFSET = -155

/**
 * Define a column typed of [DateTimeOffsetSqlType].
 */
public fun BaseTable<*>.datetimeoffset(name: String): Column<OffsetDateTime> {
    return registerColumn(name, DateTimeOffsetSqlType)
}

/**
 * [SqlType] implementation represents SQL Server `datetimeoffset` SQL type.
 */
public object DateTimeOffsetSqlType : SqlType<OffsetDateTime>(TYPE_CODE_DATETIMEOFFSET, "datetimeoffset") {
    // Access sqlserver API by reflection, because it is not a JDK 9 module,
    // we are not able to require it in module-info.java.
    private val cls = Class.forName("microsoft.sql.DateTimeOffset")
    private val valueOfMethod = cls.getMethod("valueOf", Timestamp::class.java, Int::class.javaPrimitiveType)
    private val getOffsetDateTimeMethod = cls.getMethod("getOffsetDateTime")

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: OffsetDateTime) {
        @Suppress("SwallowedException")
        try {
            val ts = Timestamp.from(parameter.toInstant())
            val offset = parameter.offset.totalSeconds / 60
            val value = valueOfMethod.invoke(null, ts, offset)
            ps.setObject(index, value, typeCode)
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }

    override fun doGetResult(rs: ResultSet, index: Int): OffsetDateTime? {
        val obj = cls.cast(rs.getObject(index))
        if (obj == null) {
            return null
        } else {
            @Suppress("SwallowedException")
            try {
                return getOffsetDateTimeMethod.invoke(obj) as OffsetDateTime
            } catch (e: InvocationTargetException) {
                throw e.targetException
            }
        }
    }
}
