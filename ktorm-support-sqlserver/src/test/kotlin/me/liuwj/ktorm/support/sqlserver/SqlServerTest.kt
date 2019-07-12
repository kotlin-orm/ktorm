package me.liuwj.ktorm.support.sqlserver

import me.liuwj.ktorm.BaseTest
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.logging.ConsoleLogger
import me.liuwj.ktorm.logging.LogLevel
import org.junit.Ignore

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
}