package me.liuwj.ktorm.support.sqlserver

import me.liuwj.ktorm.BaseTest
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.use
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.count
import me.liuwj.ktorm.entity.sequenceOf
import me.liuwj.ktorm.logging.ConsoleLogger
import me.liuwj.ktorm.logging.LogLevel
import me.liuwj.ktorm.schema.Table
import me.liuwj.ktorm.schema.datetime
import me.liuwj.ktorm.schema.varchar
import microsoft.sql.DateTimeOffset
import org.junit.ClassRule
import org.junit.Test
import org.testcontainers.containers.MSSQLServerContainer
import java.sql.Timestamp
import java.time.LocalDate

class SqlServerTest : BaseTest() {

    companion object {
        class KSqlServerContainer : MSSQLServerContainer<KSqlServerContainer>()

        @ClassRule
        @JvmField
        val sqlServer = KSqlServerContainer()
    }

    override fun init() {
        database = Database.connect(
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
            it.key to "test"
            it.value to "test value"
        }

        assert(database.sequenceOf(configs).count { it.key eq "test" } == 1)

        database.delete(configs) { it.key eq "test" }
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
            it.bar to DateTimeOffset.valueOf(Timestamp(0), 8 * 60)
        }

        for (row in database.from(Foo).select()) {
            println(row[Foo.bar])
            println(row[Foo.bar1])
        }
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

        assert(database.employees.count() == 5)
    }
}
