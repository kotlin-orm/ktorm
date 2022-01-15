package org.ktorm.support.postgresql

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.ClassRule
import org.junit.Test
import org.ktorm.BaseTest
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.dsl.from
import org.ktorm.dsl.insert
import org.ktorm.dsl.map
import org.ktorm.dsl.select
import org.ktorm.dsl.where
import org.ktorm.logging.ConsoleLogger
import org.ktorm.logging.LogLevel
import org.ktorm.schema.Table
import org.testcontainers.containers.PostgreSQLContainer

/**
 * Created by Kacper on 14/01/2022
 */
class EarthdistanceTest : BaseTest() {

    companion object {
        class KPostgreSqlContainer : PostgreSQLContainer<KPostgreSqlContainer>("postgres:13-alpine")

        @ClassRule
        @JvmField
        val postgres = KPostgreSqlContainer()
    }

    override fun init() {
        database = Database.connect(
            url = postgres.jdbcUrl,
            driver = postgres.driverClassName,
            user = postgres.username,
            password = postgres.password,
            logger = ConsoleLogger(threshold = LogLevel.TRACE)
        )

        execSqlScript("init-earthdistance.sql")
    }

    override fun destroy() {
        execSqlScript("destroy-earthdistance.sql")
    }

    @Test
    fun testEarthType() {
        val table = object : Table<Nothing>("earthdistance_t") {
            val e = earth("earth_field")
        }

        val earthValue = Earth(-849959.4557142439, -5441944.3654614175, 3216183.683045588)
        val inserted = database.insert(table) {
            set(table.e, earthValue)
        }

        val queried = database
            .from(table)
            .select()
            .where(table.e eq earthValue)
            .map { it[table.e] }
            .firstOrNull()

        assertEquals(1, inserted)
        assertEquals(earthValue, queried)
    }

    @Test
    fun testCubeType() {
        val table = object : Table<Nothing>("earthdistance_t") {
            val c = cube("cube_field")
        }

        assertThrows(IllegalArgumentException::class.java) {
            Cube(arrayOf(1.0), arrayOf(1.0, 2.0))
        }

        val cubeValue = Cube(arrayOf(-1.1, 2.2, 3.0), arrayOf(1.1, -2.2, 0.3))
        val inserted = database.insert(table) {
            set(table.c, cubeValue)
        }

        val queried = database
            .from(table)
            .select()
            .where(table.c eq cubeValue)
            .map { it[table.c] }
            .firstOrNull()

        assertEquals(1, inserted)
        assertEquals(cubeValue, queried)
    }
}