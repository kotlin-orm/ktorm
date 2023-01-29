package org.ktorm.support.sqlserver

import org.junit.Test
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Create by vince on Jul 12, 2019.
 */
class CommonTest : BaseSqlServerTest() {

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

        assert(database.from(configs).select(count()).where(configs.key eq "test").map { it.getInt(1) }[0] == 1)

        database.delete(configs) { it.key eq "test" }
    }

    /**
     * Verifies that invalid pagination parameters are ignored.
     */
    @Test
    fun testBothLimitAndOffsetAreNotPositive() {
        val query = database.from(Employees).select().orderBy(Employees.id.desc()).limit(0, -1)
        assert(query.totalRecordsInAllPages == 4)

        val ids = query.map { it[Employees.id] }
        assert(ids == listOf(4, 3, 2, 1))
    }

    /**
     * Verifies that limit parameter works as expected.
     */
    @Test
    fun testLimitWithoutOffset() {
        val query = database.from(Employees).select().orderBy(Employees.id.desc()).limit(2)
        assert(query.totalRecordsInAllPages == 4)

        val ids = query.map { it[Employees.id] }
        assert(ids == listOf(4, 3))
    }

    /**
     * Verifies that offset parameter works as expected.
     */
    @Test
    fun testOffsetWithoutLimit() {
        val query = database.from(Employees).select().orderBy(Employees.id.desc()).offset(2)
        assert(query.totalRecordsInAllPages == 4)

        val ids = query.map { it[Employees.id] }
        assert(ids == listOf(2, 1))
    }

    /**
     * Verifies that limit and offset parameters work together as expected.
     */
    @Test
    fun testOffsetWithLimit() {
        val query = database.from(Employees).select().orderBy(Employees.id.desc()).offset(2).limit(1)
        assert(query.totalRecordsInAllPages == 4)

        val ids = query.map { it[Employees.id] }
        assert(ids == listOf(2))
    }

    @Test
    fun testPagingSql() {
        var query = database
            .from(Employees)
            .leftJoin(Departments, on = Employees.departmentId eq Departments.id)
            .select(Employees.id, Employees.name)
            .orderBy(Employees.id.desc())
            .limit(0, 1)

        assert(query.totalRecordsInAllPages == 4)
        assert(query.rowSet.size() == 1)

        query = database
            .from(Employees)
            .selectDistinct(Employees.departmentId)
            .limit(0, 1)

        assert(query.totalRecordsInAllPages == 2)
        assert(query.rowSet.size() == 1)

        query = database
            .from(Employees)
            .select(Employees.name)
            .limit(0, 1)

        assert(query.totalRecordsInAllPages == 4)
        assert(query.rowSet.size() == 1)
    }

    object Foo : Table<Nothing>("foo") {
        val bar = datetimeoffset("bar")
        val bar1 = datetime("bar1")
    }

    @Test
    fun testDateTimeOffset() {
        for (row in database.from(Foo).select()) {
            assert(row[Foo.bar].toString() == "2012-10-25T12:32:10+01:00")
            assert(row[Foo.bar1].toString() == "2012-10-25T19:32:10")
        }
    }

    @Test
    fun testDateTimeOffset1() {
        database.update(Foo) {
            set(it.bar, OffsetDateTime.of(LocalDateTime.of(2023, 1, 1, 12, 30), ZoneOffset.ofHours(7)))
        }

        for (row in database.from(Foo).select()) {
            assert(row[Foo.bar].toString() == "2023-01-01T12:30+07:00")
            assert(row[Foo.bar1].toString() == "2012-10-25T19:32:10")
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
