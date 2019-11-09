package me.liuwj.ktorm.support.sqlite

import me.liuwj.ktorm.BaseTest
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.logging.ConsoleLogger
import me.liuwj.ktorm.logging.LogLevel
import org.junit.Test
import java.sql.Connection
import java.sql.DriverManager

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
}
