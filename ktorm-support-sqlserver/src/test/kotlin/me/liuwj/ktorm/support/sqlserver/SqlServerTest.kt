package me.liuwj.ktorm.support.sqlserver

import me.liuwj.ktorm.BaseTest
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.logging.ConsoleLogger
import me.liuwj.ktorm.logging.LogLevel
import org.junit.Ignore
import org.junit.Test

@Ignore
class SqlServerTest : BaseTest() {

    override fun connect() {
        Database.connect(
            url = "jdbc:sqlserver://localhost:1433;DatabaseName=ktorm",
            driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver",
            user = "ktorm",
            password = "123456",
            logger = ConsoleLogger(threshold = LogLevel.TRACE)
        )
    }

    override fun init() {
        connect()
        execSqlScript("init-sqlserver-data.sql")
    }

    @Test
    fun testPagingSql() {
        var query = Employees
            .leftJoin(Departments, on = Employees.departmentId eq Departments.id)
            .select(Employees.id, Employees.name)
            .orderBy(Employees.id.desc())
            .limit(0, 1)

        assert(query.totalRecords == 4)
        assert(query.count() == 1)

        query = Employees
            .selectDistinct(Employees.departmentId)
            .limit(0, 1)

        assert(query.totalRecords == 2)
        assert(query.count() == 1)

        query = Employees
            .select(Employees.name)
            .limit(0, 1)

        assert(query.totalRecords == 4)
        assert(query.count() == 1)
    }
}
