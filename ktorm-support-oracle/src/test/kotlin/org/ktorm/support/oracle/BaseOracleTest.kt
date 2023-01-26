package org.ktorm.support.oracle

import org.junit.ClassRule
import org.ktorm.BaseTest
import org.ktorm.database.Database
import org.testcontainers.containers.OracleContainer

abstract class BaseOracleTest : BaseTest() {

    override fun init() {
        database = Database.connect(
            url = container.jdbcUrl,
            driver = container.driverClassName,
            user = container.username,
            password = container.password,
            alwaysQuoteIdentifiers = true
        )

        execSqlScript("init-oracle-data.sql")
    }

    override fun destroy() {
        execSqlScript("drop-oracle-data.sql")
    }

    companion object {
        @JvmField
        @ClassRule
        val container = OracleContainer("zerda/oracle-database:11.2.0.2-xe")
            // At least 1 GB memory is required by Oracle.
            .withCreateContainerCmdModifier { cmd -> cmd.hostConfig?.withShmSize((1 * 1024 * 1024 * 1024).toLong()) }
    }
}