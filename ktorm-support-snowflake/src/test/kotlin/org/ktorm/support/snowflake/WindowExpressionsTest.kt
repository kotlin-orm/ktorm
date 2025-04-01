/*
 * Copyright 2025 the original author or authors.
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
 *
 * Original authors of the snowflake dialect were CarGurus: Don Mitchell, Ashish Shrestha, Mike Roberts, and others.
 */
package org.ktorm.support.snowflake

import org.junit.Before
import org.junit.Test
import org.ktorm.dsl.asc
import org.ktorm.dsl.cumeDist
import org.ktorm.dsl.from
import org.ktorm.dsl.lastValue
import org.ktorm.dsl.orderBy
import org.ktorm.dsl.partitionBy
import org.ktorm.dsl.select
import org.ktorm.dsl.window
import org.ktorm.expression.WindowFunctionType.LAST_VALUE
import org.ktorm.schema.VarcharSqlType
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WindowExpressionsTest {
    private lateinit var database: org.ktorm.database.Database

    @Before
    fun setUp() {
        database = setupMockDatabase()
    }

    @Test
    fun testGeneratesIgnoreNulls() {
        val query = database.from(SampleTable)
            .select(
                lastValue(SampleTable.name).ignoreNulls().over(
                    window().partitionBy(SampleTable.startTimestamp).orderBy(SampleTable.startTimestamp.asc())
                )
            )

        val sql = query.sql.uppercase()
        assertTrue(sql.contains("LAST_VALUE") && sql.contains("IGNORE NULLS"))
        assertFalse(sql.contains(") OVER"))
    }

    @Test
    fun testGeneratesRespectNulls() {
        val query = database.from(SampleTable)
            .select(
                lastValue(SampleTable.name).respectNulls().over(
                    window().partitionBy(SampleTable.startTimestamp).orderBy(SampleTable.startTimestamp.asc())
                )
            )

        val sql = query.sql.uppercase()
        assertTrue(sql.contains("LAST_VALUE") && sql.contains("RESPECT NULLS"))
        assertFalse(sql.contains(") OVER"))
    }

    @Test
    fun testIgnoresNone() {
        val query = database.from(SampleTable)
            .select(
                NullAwareWindowFunctionExpression(
                    LAST_VALUE,
                    listOf(SampleTable.name.asExpression()),
                    window = window().partitionBy(SampleTable.startTimestamp)
                        .orderBy(SampleTable.startTimestamp.asc()),
                    sqlType = VarcharSqlType,
                    handleNulls = null,
                )
            )

        val sql = query.sql.uppercase()
        assertTrue(sql.contains(") OVER"), "Expected to find the window function")
        assertFalse(sql.contains("IGNORE NULLS") || sql.contains("RESPECT NULLS"))
    }

    @Test
    fun testFlagsUnsupportedWindowTypes() {
        assertFailsWith<IllegalArgumentException> {
            database.from(SampleTable)
                .select(
                    cumeDist().ignoreNulls().over(
                        window().partitionBy(SampleTable.startTimestamp)
                    )
                )
        }
    }
}

