package org.ktorm.support.sqlserver

import org.ktorm.BaseTest
import org.ktorm.database.Database
import org.testcontainers.containers.MSSQLServerContainer
import kotlin.concurrent.thread

abstract class BaseSqlServerTest : BaseTest() {

    override fun init() {
        database = Database.connect(jdbcUrl, driverClassName, username, password)
        execSqlScript("init-sqlserver-data.sql")
    }

    override fun destroy() {
        execSqlScript("drop-sqlserver-data.sql")
    }

    companion object : MSSQLServerContainer<Companion>("mcr.microsoft.com/mssql/server:2017-CU12") {
        init {
            // Start the container when it's first used.
            start()
            // Stop the container when the process exits.
            Runtime.getRuntime().addShutdownHook(thread(start = false) { stop() })
        }
    }
}