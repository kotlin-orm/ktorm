package org.ktorm.support.oracle

import org.junit.ClassRule
import org.junit.Test
import org.ktorm.BaseTest
import org.ktorm.database.Database
import org.ktorm.database.use
import org.ktorm.dsl.*
import org.ktorm.entity.count
import org.ktorm.entity.filter
import org.ktorm.entity.mapTo
import org.ktorm.entity.sequenceOf
import org.ktorm.logging.ConsoleLogger
import org.ktorm.logging.LogLevel
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar
import org.testcontainers.containers.OracleContainer
import java.util.*
import kotlin.collections.HashSet

/**
 * Created by vince at Aug 01, 2020.
 */
class OracleTest : BaseTest() {

    companion object {
        const val TOTAL_RECORDS = 4
        const val MINUS_ONE = -1
        const val ZERO = 0
        const val ONE = 1
        const val TWO = 2
        const val ID_1 = 1
        const val ID_2 = 2
        const val ID_3 = 3
        const val ID_4 = 4

        @ClassRule
        @JvmField
        val oracle: OracleContainer = OracleContainer("zerda/oracle-database:11.2.0.2-xe")
            .withCreateContainerCmdModifier { cmd -> cmd.hostConfig?.withShmSize(1024L * 1024 * 1024) }
    }

    override fun init() {
        database = Database.connect(
            url = oracle.jdbcUrl,
            driver = oracle.driverClassName,
            user = oracle.username,
            password = oracle.password,
            logger = ConsoleLogger(threshold = LogLevel.TRACE),
            alwaysQuoteIdentifiers = true
        )

        execSqlScript("init-oracle-data.sql")
    }

    override fun destroy() {
        execSqlScript("drop-oracle-data.sql")
    }

    @Test
    fun testKeywordWrapping() {
        val configs = object : Table<Nothing>("T_CONFIG") {
            val key = varchar("KEY").primaryKey()
            val value = varchar("VALUE")
        }

        database.useConnection { conn ->
            conn.createStatement().use { statement ->
                val sql = """CREATE TABLE T_CONFIG("KEY" VARCHAR(128) PRIMARY KEY, "VALUE" VARCHAR(128))"""
                statement.executeUpdate(sql)
            }
        }

        database.insert(configs) {
            set(it.key, "test")
            set(it.value, "test value")
        }

        assert(database.sequenceOf(configs).count { it.key eq "test" } == 1)

        database.delete(configs) { it.key eq "test" }
    }

    @Test
    fun testLimit() {
        val query = database.from(Employees).select().orderBy(Employees.id.desc()).limit(0, 2)
        assert(query.totalRecords == 4)

        val ids = query.map { it[Employees.id] }
        assert(ids[0] == 4)
        assert(ids[1] == 3)
    }

    /**
     * Verifies that invalid pagination parameters are ignored.
     */
    @Test
    fun testBothLimitAndOffsetAreNotPositive() {
        val query = database.from(Employees).select().orderBy(Employees.id.desc()).limit(ZERO, MINUS_ONE)
        assert(query.totalRecords == TOTAL_RECORDS)

        val ids = query.map { it[Employees.id] }
        assert(ids == listOf(ID_4, ID_3, ID_2, ID_1))
    }

    /**
     * Verifies that limit parameter works as expected.
     */
    @Test
    fun testLimitWithoutOffset() {
        val query = database.from(Employees).select().orderBy(Employees.id.desc()).limit(TWO)
        assert(query.totalRecords == TOTAL_RECORDS)

        val ids = query.map { it[Employees.id] }
        assert(ids == listOf(ID_4, ID_3))
    }

    /**
     * Verifies that offset parameter works as expected.
     */
    @Test
    fun testOffsetWithoutLimit() {
        val query = database.from(Employees).select().orderBy(Employees.id.desc()).offset(TWO)
        assert(query.totalRecords == TOTAL_RECORDS)

        val ids = query.map { it[Employees.id] }
        assert(ids == listOf(ID_2, ID_1))
    }

    /**
     * Verifies that limit and offset parameters work together as expected.
     */
    @Test
    fun testOffsetWithLimit() {
        val query = database.from(Employees).select().orderBy(Employees.id.desc()).offset(TWO).limit(ONE)
        assert(query.totalRecords == TOTAL_RECORDS)

        val ids = query.map { it[Employees.id] }
        assert(ids == listOf(ID_2))
    }

    @Test
    fun testSequence() {
        for (employee in database.employees) {
            println(employee)
        }
    }

    @Test
    fun testSchema() {
        val t = object : Table<Department>(
            "t_department",
            schema = oracle.username.uppercase(Locale.getDefault())
        ) {
            val id = int("id").primaryKey().bindTo { it.id }
            val name = varchar("name").bindTo { it.name }
        }

        database.update(t) {
            set(it.name, "test")
            where {
                it.id eq 1
            }
        }

        assert(database.sequenceOf(t).filter { it.id eq 1 }.mapTo(HashSet()) { it.name } == setOf("test"))
        assert(database.sequenceOf(t.aliased("t")).mapTo(HashSet()) { it.name } == setOf("test", "finance"))
    }

    @Test
    fun testMaxColumnNameLength() {
        val t = object : Table<Nothing>("t_long_name") {
            val col = varchar("a".repeat(database.maxColumnNameLength))
        }

        database.useConnection { conn ->
            conn.createStatement().use { statement ->
                val sql = """create table "t_long_name"("${t.col.name}" varchar(128))"""
                statement.executeUpdate(sql)
            }
        }

        database.insert(t) {
            set(it.col, "test")
        }

        try {
            val name = database.from(t).select(t.col).map { it[t.col] }.first()
            println(name)
            throw java.lang.AssertionError("unexpected.")
        } catch (e: IllegalStateException) {
            println(e.message)
            assert("too long" in e.message!!)
        }
    }
}