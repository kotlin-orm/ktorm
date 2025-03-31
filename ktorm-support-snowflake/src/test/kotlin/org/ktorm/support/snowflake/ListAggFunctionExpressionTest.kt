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
import kotlin.test.assertTrue

/**
 * Test class for the ListAgg function expressions in Snowflake SQL dialect.
 * Tests the generation of LISTAGG SQL functions with various parameters.
 */
class ListAggFunctionExpressionTest {
    private lateinit var database: org.ktorm.database.Database

    /**
     * Sets up the test environment by creating a mock database connection.
     */
    @Before
    fun setUp() {
        database = setupMockDatabase()
    }

    /**
     * Tests that the basic ListAgg query is correctly generated.
     */
    @Test
    fun testHandlesBaseQuery() {
        val query = database.from(SampleTable)
            .select(listAgg(SampleTable.name))

        assertTrue(query.sql.contains("LISTAGG(WAREHOUSE.RANDOM.SAMPLE_TABLE.NAME)"))
        assertFalse(query.sql.contains("WITHIN GROUP"))
    }

    /**
     * Tests that the ListAgg query with DISTINCT option is correctly generated.
     */
    @Test
    fun testHandlesDistinctValues() {
        val query = database.from(SampleTable)
            .select(listAgg(SampleTable.name, isDistinct = true))

        assertTrue(query.sql.contains("LISTAGG(DISTINCT WAREHOUSE.RANDOM.SAMPLE_TABLE.NAME)"))
        assertFalse(query.sql.contains("WITHIN GROUP"))
    }

    /**
     * Tests that the ListAgg query with a custom separator is correctly generated.
     */
    @Test
    fun testHandlesListSeparatorArguments() {
        val query = database.from(SampleTable)
            .select(listAgg(SampleTable.name, separator = ","))

        assertTrue(query.sql.contains("LISTAGG(WAREHOUSE.RANDOM.SAMPLE_TABLE.NAME, ?)"))
        assertFalse(query.sql.contains("WITHIN GROUP"))
    }

    /**
     * Tests that the ListAgg query with ORDER BY clause is correctly generated.
     */
    @Test
    fun testHandlesWithinGroupOrderingArguments() {
        val query = database.from(SampleTable)
            .select(listAgg(SampleTable.name)
                .orderBy(listOf(SampleTable.id.asc(), SampleTable.endTimestamp.desc())))

        assertTrue(query.sql.contains("LISTAGG(WAREHOUSE.RANDOM.SAMPLE_TABLE.NAME) WITHIN"))
        assertTrue(query.sql.contains("WITHIN GROUP (ORDER BY WAREHOUSE.RANDOM.SAMPLE_TABLE.ID, "))
        assertTrue(query.sql.contains(", WAREHOUSE.RANDOM.SAMPLE_TABLE.endTimestamp DESC)"))
    }

    /**
     * Helper function to improve readability in tests.
     */
    private fun assertFalse(condition: Boolean) {
        kotlin.test.assertFalse(condition, "Condition should be false")
    }
}

