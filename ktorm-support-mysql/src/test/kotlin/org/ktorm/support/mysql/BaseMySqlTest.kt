package org.ktorm.support.mysql

import org.ktorm.BaseTest
import org.ktorm.database.Database
import org.testcontainers.containers.MySQLContainer
import kotlin.concurrent.thread

abstract class BaseMySqlTest : BaseTest() {

    override fun init() {
        database = Database.connect(jdbcUrl, driverClassName, username, password)
        execSqlScript("init-mysql-data.sql")
    }

    override fun destroy() {
        execSqlScript("drop-mysql-data.sql")
    }

    companion object : MySQLContainer<Companion>("mysql:8") {
        init {
            // Start the container when it's first used.
            start()
            // Stop the container when the process exits.
            Runtime.getRuntime().addShutdownHook(thread(start = false) { stop() })
        }
    }
}