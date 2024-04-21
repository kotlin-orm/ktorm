package org.ktorm.support.oracle

import org.ktorm.BaseTest
import org.ktorm.database.Database
import org.testcontainers.containers.OracleContainer
import kotlin.concurrent.thread

abstract class BaseOracleTest : BaseTest() {

    override fun init() {
        database = Database.connect(jdbcUrl, driverClassName, username, password, alwaysQuoteIdentifiers = true)
        execSqlScript("init-oracle-data.sql")
    }

    override fun destroy() {
        execSqlScript("drop-oracle-data.sql")
    }

    /**
     * Unfortunately Oracle databases aren’t compatible with the new Apple Silicon CPU architecture,
     * so if you are using a brand-new MacBook, you need to install colima.
     *
     * 1. Installation: https://github.com/abiosoft/colima#installation
     * 2. Run Colima with the command: `colima start --arch x86_64 --cpu 2 --memory 4 --disk 16 --network-address`
     * 3. Set env vars like below:
     *
     * ```sh
     * export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
     * export TESTCONTAINERS_HOST_OVERRIDE=$(colima ls -j | jq -r '.address')
     * export DOCKER_HOST="unix://${HOME}/.colima/default/docker.sock"
     * ```
     *
     * See https://java.testcontainers.org/supported_docker_environment/#colima
     */
    companion object : OracleContainer("gvenzl/oracle-xe:11.2.0.2") {
        init {
            // Configure the container.
            usingSid()
            withCreateContainerCmdModifier { cmd -> cmd.hostConfig?.withShmSize((1 * 1024 * 1024 * 1024).toLong()) }

            // Start the container when it's first used.
            start()
            // Stop the container when the process exits.
            Runtime.getRuntime().addShutdownHook(thread(start = false) { stop() })
        }
    }
}