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

import org.junit.Test
import org.ktorm.dsl.*
import org.ktorm.schema.*
import kotlin.test.assertEquals

object SampleUpdateTable : Table<Nothing>("OTHERTABLE", schema = "RANDOM") {
    val id = long("ID").primaryKey()
    val sampleName = varchar("SAMPLENAME")
    val sampleId = long("SAMPLEID")
}

class UpdateSnowflakeTest {
    private val database = setupMockDatabase()

    @Test
    fun testUpdateFromSqlGeneration() {
        val expression = database.updateFrom(
            SampleUpdateTable,
            SampleTable
        ) {
            set(SampleUpdateTable.sampleId, SampleTable.id)
        }
            .where {
                SampleTable.name eq SampleUpdateTable.sampleName
            }

        val formatter = SnowflakeFormatter(database, false, 2)
        formatter.visit(expression)

        val expectedSql = "update random.othertable from warehouse.random.sample_table " +
                "set sampleid = warehouse.random.sample_table.id " +
                "where warehouse.random.sample_table.name = random.othertable.samplename "

        assertEquals(expectedSql, formatter.sql.lowercase())
    }
}

