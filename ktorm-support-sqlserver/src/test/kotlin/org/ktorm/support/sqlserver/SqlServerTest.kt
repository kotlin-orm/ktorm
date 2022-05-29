package org.ktorm.support.sqlserver

import microsoft.sql.DateTimeOffset
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
import org.ktorm.schema.Table
import org.ktorm.schema.datetime
import org.ktorm.schema.int
import org.ktorm.schema.varchar
import org.testcontainers.containers.MSSQLServerContainer
import java.sql.Timestamp
import java.time.LocalDate

/**
 * Create by vince on Jul 12, 2019.
 */
class SqlServerTest : BaseTest() {

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

        class KSqlServerContainer : MSSQLServerContainer<KSqlServerContainer>("mcr.microsoft.com/mssql/server:2017-CU12")

        @ClassRule
        @JvmField
        val sqlServer = KSqlServerContainer()
    }

    override fun init() {
        database = Database.connect(sqlServer.jdbcUrl, sqlServer.driverClassName, sqlServer.username, sqlServer.password)
        execSqlScript("init-sqlserver-data.sql")
    }

    override fun destroy() {
        execSqlScript("drop-sqlserver-data.sql")
    }

    @Test
    fun testKeywordWrapping() {
        val configs = object : Table<Nothing>("t_config") {
            val key = varchar("key").primaryKey()
            val value = varchar("value")
        }

        database.useConnection { conn ->
            conn.createStatement().use { statement ->
                val sql = """create table t_config("key" varchar(128) primary key, "value" varchar(128))"""
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
    fun testPagingSql() {
        var query = database
            .from(Employees)
            .leftJoin(Departments, on = Employees.departmentId eq Departments.id)
            .select(Employees.id, Employees.name)
            .orderBy(Employees.id.desc())
            .limit(0, 1)

        assert(query.totalRecords == 4)
        assert(query.rowSet.size() == 1)

        query = database
            .from(Employees)
            .selectDistinct(Employees.departmentId)
            .limit(0, 1)

        assert(query.totalRecords == 2)
        assert(query.rowSet.size() == 1)

        query = database
            .from(Employees)
            .select(Employees.name)
            .limit(0, 1)

        assert(query.totalRecords == 4)
        assert(query.rowSet.size() == 1)
    }

    object Foo : Table<Nothing>("foo") {
        val bar = datetimeoffset("bar")
        val bar1 = datetime("bar1")
    }

    @Test
    fun testGetColumn() {
        database.update(Foo) {
            set(it.bar, DateTimeOffset.valueOf(Timestamp(0), 8 * 60))
        }

        for (row in database.from(Foo).select()) {
            println(row[Foo.bar])
            println(row[Foo.bar1])
        }
    }

    @Test
    fun testInsertAndGenerateKey() {
        val id = database.insertAndGenerateKey(Employees) {
            set(it.name, "Joe Friend")
            set(it.job, "Tester")
            set(it.managerId, null)
            set(it.salary, 50)
            set(it.hireDate, LocalDate.of(2020, 1, 10))
            set(it.departmentId, 1)
        } as Int

        assert(id > 4)

        assert(database.employees.count() == 5)
    }

    @Test
    fun testSchema() {
        val t = object : Table<Department>("t_department", schema = "dbo") {
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
                val sql = """create table t_long_name(${t.col.name} varchar(128))"""
                statement.executeUpdate(sql)
            }
        }

        database.insert(t) {
            set(it.col, "test")
        }

        try {
            val name = database.from(t).select(t.col).map { it[t.col] }.first()
            println(name)
            throw AssertionError("unexpected.")
        } catch (e: IllegalStateException) {
            println(e.message)
            assert("too long" in e.message!!)
        }
    }

    @Test
    fun testSequence() {
        for (employee in database.employees) {
            println(employee)
        }
    }
}
