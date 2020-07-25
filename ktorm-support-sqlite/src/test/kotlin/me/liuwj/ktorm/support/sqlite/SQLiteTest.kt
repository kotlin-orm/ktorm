package me.liuwj.ktorm.support.sqlite

import me.liuwj.ktorm.BaseTest
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.use
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.count
import me.liuwj.ktorm.entity.find
import me.liuwj.ktorm.entity.sequenceOf
import me.liuwj.ktorm.logging.ConsoleLogger
import me.liuwj.ktorm.logging.LogLevel
import me.liuwj.ktorm.schema.Table
import me.liuwj.ktorm.schema.varchar
import org.junit.Test
import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDate

/**
 * Created by vince on Dec 12, 2018.
 */
class SQLiteTest : BaseTest() {

    lateinit var connection: Connection

    override fun init() {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:")

        database = Database.connect(logger = ConsoleLogger(LogLevel.TRACE)) {
            object : Connection by connection {
                override fun close() {
                    // do nothing...
                }
            }
        }

        execSqlScript("init-sqlite-data.sql")
    }

    override fun destroy() {
        super.destroy()
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
            it.key to "test"
            it.value to "test value"
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
            it.name to "Joe Friend"
            it.job to "Tester"
            it.managerId to null
            it.salary to 50
            it.hireDate to LocalDate.of(2020, 1, 10)
            it.departmentId to 1
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
