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

import io.mockk.every
import io.mockk.mockk
import org.ktorm.database.Database
import org.ktorm.logging.detectLoggerImplementation
import org.ktorm.schema.*
import java.sql.*
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

object SampleTable : Table<Nothing>(tableName = "SAMPLE_TABLE", schema = "RANDOM", catalog = "WAREHOUSE") {
    val id = long("ID")
    val name = varchar("NAME")
    var entryDate = datetime("ENTRYDATE")
    val otherName = varchar("OTHERNAME")
    val startTimestamp = datetime("startTimestamp")
    val endTimestamp = datetime("endTimestamp")
    val jsonObject = varchar("JSONOBJECT")
}

fun setupMockDatabase(): Database {
    val connection = mockk<Connection>(relaxUnitFun = true)
    every { connection.close() } returns mockk()
    every { connection.metaData } returns mockk()

    return Database.connect(
        dialect = SnowflakeDialect(),
        logger = detectLoggerImplementation(),
        connector = fun(): Connection { return connection }
    )
}

class SnowflakeDialectTest {
    private lateinit var database: org.ktorm.database.Database

    @Before
    fun setUp() {
        database = setupMockDatabase()
    }

    @Test
    internal fun `handles sql for date_trunc`() {
        val formatter = SnowflakeFormatter(database, false, 2)
        formatter.visit(SampleTable.entryDate.dateTrunc(DatePart.DAY))
        assertEquals("DATE_TRUNC(?, WAREHOUSE.RANDOM.SAMPLE_TABLE.ENTRYDATE) ", formatter.sql)
    }

    @Test
    internal fun `handles sql for to_date`() {
        val formatter = SnowflakeFormatter(database, false, 2)
        formatter.visit(SampleTable.entryDate.toDate())
        assertEquals("TO_DATE(WAREHOUSE.RANDOM.SAMPLE_TABLE.ENTRYDATE) ", formatter.sql)
    }

    @Test
    internal fun `format hour sql`() {
        val formatter = SnowflakeFormatter(database, false, 2)
        formatter.visit(SampleTable.startTimestamp.hour())
        assertEquals("HOUR(WAREHOUSE.RANDOM.SAMPLE_TABLE.startTimestamp) ", formatter.sql)
    }

    @Test
    internal fun `format datediff snowflake sql`() {
        val formatter = SnowflakeFormatter(database, false, 2)
        formatter.visit(SampleTable.startTimestamp.dateDiff(TimePart.HOUR, SampleTable.endTimestamp))
        assertEquals("DATEDIFF(?, WAREHOUSE.RANDOM.SAMPLE_TABLE.startTimestamp, " +
                "WAREHOUSE.RANDOM.SAMPLE_TABLE.endTimestamp) ", formatter.sql)
    }

    @Test
    internal fun `handles sql for decode without default`() {
        val formatter = SnowflakeFormatter(database, false, 2)
        formatter.visit(SampleTable.name.decode<String, Int>(mapOf("foo" to 1, "bar" to 2), IntSqlType))
        assertEquals("DECODE(WAREHOUSE.RANDOM.SAMPLE_TABLE.NAME, ?, ?, ?, ?) ", formatter.sql)
    }

    @Test
    internal fun `handles sql for decode with default`() {
        val formatter = SnowflakeFormatter(database, false, 2)
        formatter.visit(SampleTable.name.decode<String, Int>(mapOf("foo" to 1, "bar" to 2), 3, IntSqlType))
        assertEquals("DECODE(WAREHOUSE.RANDOM.SAMPLE_TABLE.NAME, ?, ?, ?, ?, ?) ", formatter.sql)
    }

    @Test
    internal fun `formats null value properly`() {
        val formatter = SnowflakeFormatter(database, false, 2)
        formatter.visit(NullExpression<Int>(IntSqlType))
        assertEquals("null ", formatter.sql)
    }

    @Test
    internal fun `handles sql for default as column`() {
        val formatter = SnowflakeFormatter(database, false, 2)
        formatter.visit(SampleTable.name.nvl<String>(SampleTable.otherName))
        assertEquals(
            "NVL(WAREHOUSE.RANDOM.SAMPLE_TABLE.NAME, WAREHOUSE.RANDOM.SAMPLE_TABLE.OTHERNAME) ",
            formatter.sql
        )
    }

    @Test
    internal fun `handles sql for default as value`() {
        val formatter = SnowflakeFormatter(database, false, 2)
        formatter.visit(SampleTable.name.nvl<String>("default"))
        assertEquals("NVL(WAREHOUSE.RANDOM.SAMPLE_TABLE.NAME, ?) ", formatter.sql)
    }
}
