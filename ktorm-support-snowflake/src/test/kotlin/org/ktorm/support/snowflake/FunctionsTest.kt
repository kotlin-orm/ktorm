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
import org.ktorm.dsl.*
import org.ktorm.schema.*
import kotlin.random.Random
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FunctionsTest {
    private lateinit var database: org.ktorm.database.Database

    @Before
    fun setUp() {
        database = setupMockDatabase()
    }

    @Test
    fun testConcatJoinsStrings() {
        val query = database.from(SampleTable)
            .select(SampleTable.entryDate, concat(SampleTable.name, ", ", SampleTable.otherName))
        assertTrue(query.sql.contains("CONCAT("))
        assertTrue(query.sql.contains("?"))
        assertTrue(query.sql.contains(SampleTable.name.name))
        assertTrue(query.sql.contains(SampleTable.otherName.name))
    }

    @Test
    fun testGeneratesSha2() {
        val query = database.from(SampleTable)
            .select(SampleTable.name.sha2())

        val sql = query.sql
        assertTrue(sql.contains("SHA2") && sql.contains(","))
    }

    @Test
    fun testRejectsUnsupportedDigestSizes() {
        assertFailsWith<IllegalArgumentException> {
            database.from(SampleTable)
                .select(SampleTable.name.sha2(12))
        }
    }

    @Test
    fun testDateAdd() {
        val days = Random.nextInt()
        val query = database.from(SampleTable)
            .select(
                SampleTable.entryDate.toDate().dateAdd(DatePart.DAY, days))
        assertTrue(query.sql.contains("DATEADD(?, ?, "))
        assertTrue(query.sql.contains("SAMPLE_TABLE.ENTRYDATE"))
    }

    @Test
    fun testUsesGetToAccessObjectElement() {
        val query = database.from(SampleTable)
            .select(SampleTable.jsonObject.get("externalId", LongSqlType))
        assertTrue(query.sql.contains("GET("))
        assertTrue(query.sql.contains("SAMPLE_TABLE.JSONOBJECT, ?)"))
    }

    @Test
    fun testHandlesToCharForNumericFields() {
        val query = database.from(SampleTable)
            .select(toChar(4L), toChar(6.3), toChar(10))

        assertTrue(query.sql.contains("TO_CHAR(?), TO_CHAR(?), TO_CHAR(?)"))
    }

    @Test
    fun testHandlesToCharForColumns() {
        val query = database.from(SampleTable)
            .select(toChar(SampleTable.id))

        assertTrue(query.sql.contains("TO_CHAR(WAREHOUSE.RANDOM.SAMPLE_TABLE.ID)"))
    }
}

