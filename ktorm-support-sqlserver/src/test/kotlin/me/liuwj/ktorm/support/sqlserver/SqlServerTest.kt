package me.liuwj.ktorm.support.sqlserver

import me.liuwj.ktorm.BaseTest
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.logging.ConsoleLogger
import me.liuwj.ktorm.logging.LogLevel
import me.liuwj.ktorm.schema.Table
import me.liuwj.ktorm.schema.datetime
import microsoft.sql.DateTimeOffset
import org.junit.ClassRule
import org.junit.Test
import org.testcontainers.containers.MSSQLServerContainer
import java.sql.Timestamp

class SqlServerTest : BaseTest() {

    companion object {
        class KSqlServerContainer : MSSQLServerContainer<KSqlServerContainer>()

        @ClassRule
        @JvmField
        val sqlServer = KSqlServerContainer()
    }

    override fun init() {
        Database.connect(
            url = sqlServer.jdbcUrl,
            driver = sqlServer.driverClassName,
            user = sqlServer.username,
            password = sqlServer.password,
            logger = ConsoleLogger(threshold = LogLevel.TRACE)
        )

        execSqlScript("init-sqlserver-data.sql")
    }

    override fun destroy() {
        execSqlScript("drop-sqlserver-data.sql")
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

    object Foo : Table<Nothing>("foo") {
        val bar by datetimeoffset("bar")
        val bar1 by datetime("bar1")
    }

    @Test
    fun testGetColumn() {
        Foo.update {
            it.bar to DateTimeOffset.valueOf(Timestamp(0), 8 * 60)
        }

        for (row in Foo.select()) {
            println(row[Foo.bar])
            println(row[Foo.bar1])
        }
    }
}
