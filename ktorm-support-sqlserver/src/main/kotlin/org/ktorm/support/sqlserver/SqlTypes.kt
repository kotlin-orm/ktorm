/*
 * Copyright 2018-2022 the original author or authors.
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

import microsoft.sql.DateTimeOffset
import microsoft.sql.Types
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import org.ktorm.schema.SqlType
import java.sql.PreparedStatement
import java.sql.ResultSet

/**
 * Define a column typed of [DateTimeOffsetSqlType].
 */
public fun BaseTable<*>.datetimeoffset(name: String): Column<DateTimeOffset> {
    return registerColumn(name, DateTimeOffsetSqlType)
}

/**
 * [SqlType] implementation represents SQL Server `datetimeoffset` SQL type.
 */
public object DateTimeOffsetSqlType : SqlType<DateTimeOffset>(Types.DATETIMEOFFSET, "datetimeoffset") {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: DateTimeOffset) {
        ps.setObject(index, parameter, typeCode)
    }

    override fun doGetResult(rs: ResultSet, index: Int): DateTimeOffset? {
        return rs.getObject(index) as DateTimeOffset?
    }
}
