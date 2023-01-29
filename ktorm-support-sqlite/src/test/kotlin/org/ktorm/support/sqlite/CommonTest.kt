package org.ktorm.support.sqlite

import org.junit.Test
import org.ktorm.database.use
import org.ktorm.dsl.*
import org.ktorm.entity.*
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar
import java.time.LocalDate
import kotlin.test.assertEquals

/**
 * Created by vince on Dec 12, 2018.
 */
class CommonTest : BaseSQLiteTest() {

    @Test
    fun testKeywordWrapping() {
        val configs = object : Table<Nothing>("t_config") {
            val key = varchar("key").primaryKey()
            val value = varchar("value")
        }

        database.useConnection { conn ->
            conn.createStatement().use { statement ->
                val sql = """create table t_config(key varchar(128) primary key, value varchar(128))"""
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

    @Test
    fun testLimit() {
        val query = database.from(Employees).select().orderBy(Employees.id.desc()).limit(0, 2)
        assert(query.totalRecordsInAllPages == 4)

        val ids = query.map { it[Employees.id] }
        assert(ids.size == 2)
        assert(ids[0] == 4)
        assert(ids[1] == 3)
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
            .select()
            .orderBy(Departments.id.desc())
            .limit(0, 1)

        assert(query.totalRecordsInAllPages == 4)

        query = database
            .from(Employees)
            .select(Employees.name)
            .orderBy((Employees.id + 1).desc())
            .limit(0, 1)

        assert(query.totalRecordsInAllPages == 4)

        query = database
            .from(Employees)
            .select(Employees.departmentId, avg(Employees.salary))
            .groupBy(Employees.departmentId)
            .limit(0, 1)

        assert(query.totalRecordsInAllPages == 2)

        query = database
            .from(Employees)
            .selectDistinct(Employees.departmentId)
            .limit(0, 1)

        assert(query.totalRecordsInAllPages == 2)

        query = database
            .from(Employees)
            .select(max(Employees.salary))
            .limit(0, 1)

        assert(query.totalRecordsInAllPages == 1)

        query = database
            .from(Employees)
            .select(Employees.name)
            .limit(0, 1)

        assert(query.totalRecordsInAllPages == 4)
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

        println(database.employees.find { it.id eq id })
        assert(database.employees.count() == 5)
    }

    @Test
    fun testSequence() {
        for (employee in database.employees) {
            println(employee)
        }
    }

    interface TestMultiGeneratedKey : Entity<TestMultiGeneratedKey> {
        var id: Int
        var k: String
        var v: String
    }

    object TestMultiGeneratedKeys : Table<TestMultiGeneratedKey>("t_multi_generated_key") {
        val id = int("id").primaryKey().bindTo { it.id }
        val k = varchar("k").bindTo { it.k }
        val v = varchar("v").bindTo { it.v }
    }

    @Test
    fun testMultiGeneratedKey() {
        val e = Entity.create<TestMultiGeneratedKey>()
        e.v = "test~~"
        database.sequenceOf(TestMultiGeneratedKeys).add(e)

        val e1 = database.sequenceOf(TestMultiGeneratedKeys).first()
        println(e1)
        assertEquals(1, e1.id)
        assertEquals("test~~", e1.v)
        assert(e1.k.isNotEmpty())
    }
}
