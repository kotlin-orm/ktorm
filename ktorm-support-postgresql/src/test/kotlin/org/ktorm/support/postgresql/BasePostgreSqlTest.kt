package org.ktorm.support.postgresql

import org.ktorm.BaseTest
import org.ktorm.database.Database
import org.ktorm.logging.ConsoleLogger
import org.ktorm.logging.LogLevel
import org.testcontainers.containers.PostgreSQLContainer
import kotlin.concurrent.thread

open class BasePostgreSqlTest : BaseTest() {

    override fun init() {
        database = Database.connect(
            url = jdbcUrl,
            driver = driverClassName,
            user = username,
            password = password,
            logger = ConsoleLogger(threshold = LogLevel.TRACE)
        )

        execSqlScript("init-postgresql-data.sql")
    }

    override fun destroy() {
        execSqlScript("drop-postgresql-data.sql")
    }

    companion object : PostgreSQLContainer<Companion>("postgres:13-alpine") {
        init {
            // Start the container when it's first used.
            start()
            // Stop the container when the process exits.
            Runtime.getRuntime().addShutdownHook(thread(start = false) { stop() })
        }
    }
}