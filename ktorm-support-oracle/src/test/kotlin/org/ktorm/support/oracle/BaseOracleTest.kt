package org.ktorm.support.oracle

import org.ktorm.BaseTest
import org.ktorm.database.Database
import org.testcontainers.containers.OracleContainer
import java.sql.Connection
import java.sql.DriverManager
import kotlin.concurrent.thread

abstract class BaseOracleTest : BaseTest() {

    override fun init() {
        database = Database.connect(alwaysQuoteIdentifiers = true) {
            object : Connection by connection {
                override fun close() {
                    // do nothing...
                }
            }
        }

        execSqlScript("init-oracle-data.sql")
    }

    override fun destroy() {
        execSqlScript("drop-oracle-data.sql")
    }

    companion object : OracleContainer("zerda/oracle-database:11.2.0.2-xe") {
        lateinit var connection: Connection

        init {
            // At least 1 GB memory is required by Oracle.
            withCreateContainerCmdModifier { cmd -> cmd.hostConfig?.withShmSize((1 * 1024 * 1024 * 1024).toLong()) }
            // Start the container when it's first used.
            start()
            // Stop the container when the process exits.
            Runtime.getRuntime().addShutdownHook(thread(start = false) { stop() })
        }

        override fun start() {
            super.start()

            Class.forName(driverClassName)
            connection = DriverManager.getConnection(jdbcUrl, username, password)
        }

        override fun stop() {
            connection.close()
            super.stop()
        }
    }
}