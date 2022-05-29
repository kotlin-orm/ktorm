package org.ktorm.support.sqlite

import org.junit.Test
import org.ktorm.BaseTest
import org.ktorm.database.Database
import org.ktorm.database.use
import org.ktorm.dsl.*
import org.ktorm.entity.count
import org.ktorm.entity.find
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.varchar
import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDate

/**
 * Created by vince on Dec 12, 2018.
 */
class SQLiteTest : BaseTest() {

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
    }

    lateinit var connection: Connection

    override fun init() {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:")

        database = Database.connect {
            object : Connection by connection {
                override fun close() {
                    // do nothing...
                }
            }
        }

        execSqlScript("init-sqlite-data.sql")
    }

    override fun destroy() {
        execSqlScript("drop-sqlite-data.sql")
        connection.close()
    }

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

        assert(database.sequenceOf(configs).count { it.key eq "test" } == 1)

        database.delete(configs) { it.key eq "test" }
    }

    @Test
    fun testLimit() {
        val query = database.from(Employees).select().orderBy(Employees.id.desc()).limit(0, 2)
        assert(query.totalRecords == 4)

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
            .select()
            .orderBy(Departments.id.desc())
            .limit(0, 1)

        assert(query.totalRecords == 4)

        query = database
            .from(Employees)
            .select(Employees.name)
            .orderBy((Employees.id + 1).desc())
            .limit(0, 1)

        assert(query.totalRecords == 4)

        query = database
            .from(Employees)
            .select(Employees.departmentId, avg(Employees.salary))
            .groupBy(Employees.departmentId)
            .limit(0, 1)

        assert(query.totalRecords == 2)

        query = database
            .from(Employees)
            .selectDistinct(Employees.departmentId)
            .limit(0, 1)

        assert(query.totalRecords == 2)

        query = database
            .from(Employees)
            .select(max(Employees.salary))
            .limit(0, 1)

        assert(query.totalRecords == 1)

        query = database
            .from(Employees)
            .select(Employees.name)
            .limit(0, 1)

        assert(query.totalRecords == 4)
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
}
