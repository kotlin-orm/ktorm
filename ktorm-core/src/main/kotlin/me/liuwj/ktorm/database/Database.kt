/*
 * Copyright 2018-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.liuwj.ktorm.database

import me.liuwj.ktorm.database.Database.Companion.connect
import me.liuwj.ktorm.database.Database.Companion.connectWithSpringSupport
import me.liuwj.ktorm.expression.ArgumentExpression
import me.liuwj.ktorm.expression.SqlExpression
import me.liuwj.ktorm.logging.Logger
import me.liuwj.ktorm.logging.detectLoggerImplementation
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator
import org.springframework.transaction.annotation.Transactional
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.DriverManager
import java.sql.SQLException
import java.util.concurrent.atomic.AtomicReference
import javax.sql.DataSource

/**
 * The entry class of Ktorm, represents a physical database, used to manage connections and transactions.
 *
 * To create instances of this class, we need to call the [connect] functions on the companion object.
 * Ktorm also provides Spring support, to enable this feature, instances should be created by
 * [connectWithSpringSupport] instead.
 *
 * The connect functions returns a new-created database object, you can define a variable to save the returned
 * value if needed. But generally, it’s not necessary to do that, because Ktorm will save the latest created
 * instance automatically, then obtain it via [Database.global] when needed.
 *
 * But sometimes, we have to operate many databases in one App, so it’s needed to create many instances
 * and choose one while performing our database specific operations. In this case, the [invoke] operator
 * should be used to switch among those databases.
 *
 * ```kotlin
 * val mysql = Database.connect("jdbc:mysql://localhost:3306/ktorm", driver = "com.mysql.jdbc.Driver")
 * val h2 = Database.connect("jdbc:h2:mem:ktorm;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
 * mysql {
 *     assert(Database.global === mysql)
 *     // Use MySQL database
 * }
 * h2 {
 *     assert(Database.global === h2)
 *     // Use H2 database
 * }
 * ```
 *
 * @property transactionManager the transaction manager used to manage connections and transactions.
 * @property dialect the dialect implementation, by default, [StandardDialect] is used.
 * @property logger the logger used to output logs, printed to the console by default, pass null to disable logging.
 * @property exceptionTranslator function used to translate SQL exceptions so as to rethrow them to users.
 */
class Database(
    val transactionManager: TransactionManager,
    val dialect: SqlDialect = StandardDialect,
    val logger: Logger? = detectLoggerImplementation(),
    val exceptionTranslator: (SQLException) -> Throwable = { it }
) {
    /**
     * The URL of the connected database.
     */
    lateinit var url: String private set

    /**
     * The name of the connected database.
     */
    lateinit var name: String private set

    /**
     * The name of the connected database product, eg. MySQL, H2.
     */
    lateinit var productName: String private set

    /**
     * The version of the connected database product.
     */
    lateinit var productVersion: String private set

    /**
     * A set of all of this database's SQL keywords (including SQL:2003 keywords), all in uppercase.
     */
    lateinit var keywords: Set<String> private set

    /**
     * The string used to quote SQL identifiers, returns an empty string if identifier quoting is not supported.
     */
    lateinit var identifierQuoteString: String private set

    /**
     * All the "extra" characters that can be used in unquoted identifier names (those beyond a-z, A-Z, 0-9 and _).
     */
    lateinit var extraNameCharacters: String private set

    init {
        lastConnected.set(this)

        useMetadata { metadata ->
            url = metadata.url
            name = url.substringAfterLast('/').substringBefore('?')
            productName = metadata.databaseProductName
            productVersion = metadata.databaseProductVersion
            keywords = ANSI_SQL_2003_KEYWORDS + metadata.sqlKeywords.toUpperCase().split(',')
            identifierQuoteString = metadata.identifierQuoteString.trim()
            extraNameCharacters = metadata.extraNameCharacters
        }

        if (logger != null && logger.isInfoEnabled()) {
            logger.info("Connected to $url, productName: $productName, " +
                "productVersion: $productVersion, logger: $logger, dialect: $dialect")
        }
    }

    /**
     * Obtain a connection and invoke the callback function with its [DatabaseMetaData],
     * the connection will be automatically closed after the callback completes.
     */
    inline fun <T> useMetadata(func: (DatabaseMetaData) -> T): T {
        useConnection { conn ->
            return func(conn.metaData)
        }
    }

    /**
     * Obtain a connection and invoke the callback function with it.
     *
     * If the current thread has opened a transaction, then this transaction's connection will be used.
     * Otherwise, Ktorm will pass a new-created connection to the function and auto close it after it's
     * not useful anymore.
     */
    inline fun <T> useConnection(func: (Connection) -> T): T {
        try {
            transactionManager.currentTransaction?.let {
                return func(it.connection)
            } ?: transactionManager.newConnection().use {
                return func(it)
            }
        } catch (e: SQLException) {
            throw exceptionTranslator.invoke(e)
        }
    }

    /**
     * Execute the specific callback function in a transaction and returns its result if the execution succeeds,
     * otherwise, if the execution fails, the transaction will be rollback.
     *
     * Note:
     *
     * - Any exceptions thrown in the callback function can trigger a rollback.
     * - This function is reentrant, so it can be called nested. However, the inner calls don’t open new transactions
     * but share the same ones with outers.
     *
     * @param isolation transaction isolation, enums defined in [TransactionIsolation].
     * @param func the executed callback function.
     * @return the result of the callback function.
     */
    inline fun <T> useTransaction(
        isolation: TransactionIsolation = TransactionIsolation.REPEATABLE_READ,
        func: (Transaction) -> T
    ): T {
        val current = transactionManager.currentTransaction
        val isOuter = current == null
        val transaction = current ?: transactionManager.newTransaction(isolation)

        try {
            val result = func(transaction)
            if (isOuter) transaction.commit()
            return result
        } catch (e: SQLException) {
            if (isOuter) transaction.rollback()
            throw exceptionTranslator.invoke(e)
        } catch (e: Throwable) {
            if (isOuter) transaction.rollback()
            throw e
        } finally {
            if (isOuter) transaction.close()
        }
    }

    /**
     * Execute the callback function using the current database instance.
     *
     * Useful when we have many database instances. Call this function to choose one to execute
     * our database specific operations. While the callback functions are executing, the [Database.global]
     * property will be set to the current database. And after the callback completes, it's automatically
     * restored to the origin one.
     *
     * @see Database.global
     */
    operator fun <T> invoke(func: Database.() -> T): T {
        val origin = threadLocal.get()

        try {
            threadLocal.set(this)
            return this.func()
        } catch (e: SQLException) {
            throw exceptionTranslator.invoke(e)
        } finally {
            origin?.let { threadLocal.set(it) } ?: threadLocal.remove()
        }
    }

    /**
     * Format the specific [SqlExpression] to an executable SQL string with execution arguments.
     *
     * @param expression the expression to be formatted.
     * @param beautifySql output beautiful SQL strings with line-wrapping and indentation, default to `false`.
     * @param indentSize the indent size, default to 2.
     * @return a [Pair] combines the SQL string and its execution arguments.
     */
    fun formatExpression(
        expression: SqlExpression,
        beautifySql: Boolean = false,
        indentSize: Int = 2
    ): Pair<String, List<ArgumentExpression<*>>> {

        val formatter = dialect.createSqlFormatter(database = this, beautifySql = beautifySql, indentSize = indentSize)
        formatter.visit(expression)
        return formatter.sql to formatter.parameters
    }

    /**
     * Companion object provides functions to connect to databases and holds the [global] database instances.
     */
    companion object {
        private val lastConnected = AtomicReference<Database>()
        private val threadLocal = ThreadLocal<Database>()

        /**
         * The global database instance, Ktorm uses this property to obtain a database when any SQL is executed.
         *
         * By default, it's the lasted connected one, but it may change if the [invoke] operator is used.
         *
         * @see invoke
         */
        val global get() = threadLocal.get() ?: lastConnected.get() ?: error("Not connected to any database yet.")

        /**
         * Connect to a database by a specific [connector] function.
         *
         * @param dialect the dialect implementation, by default, [StandardDialect] is used.
         * @param logger the logger used to output logs, printed to the console by default, null to disable logging.
         * @param connector the connector function used to obtain SQL connections.
         * @return the new-created database object.
         */
        fun connect(
            dialect: SqlDialect = StandardDialect,
            logger: Logger? = detectLoggerImplementation(),
            connector: () -> Connection
        ): Database {
            return Database(JdbcTransactionManager(connector), dialect, logger)
        }

        /**
         * Connect to a database using a [DataSource].
         *
         * @param dataSource the data source used to obtain SQL connections.
         * @param dialect the dialect implementation, by default, [StandardDialect] is used.
         * @param logger the logger used to output logs, printed to the console by default, null to disable logging.
         * @return the new-created database object.
         */
        fun connect(
            dataSource: DataSource,
            dialect: SqlDialect = StandardDialect,
            logger: Logger? = detectLoggerImplementation()
        ): Database {
            return connect(dialect, logger) { dataSource.connection }
        }

        /**
         * Connect to a database using the specific connection arguments.
         *
         * @param url the URL of the database to be connected.
         * @param driver the full qualified name of the JDBC driver class.
         * @param user the user name of the database.
         * @param password the password of the database.
         * @param dialect the dialect implementation, by default, [StandardDialect] is used.
         * @param logger the logger used to output logs, printed to the console by default, null to disable logging.
         * @return the new-created database object.
         */
        fun connect(
            url: String,
            driver: String,
            user: String = "",
            password: String = "",
            dialect: SqlDialect = StandardDialect,
            logger: Logger? = detectLoggerImplementation()
        ): Database {
            Class.forName(driver)
            return connect(dialect, logger) { DriverManager.getConnection(url, user, password) }
        }

        /**
         * Connect to a database using a [DataSource] with the Spring support enabled.
         *
         * Once the Spring support enabled, the transaction management will be delegated to the Spring framework,
         * so the [useTransaction] function is not available anymore, we need to use Spring's [Transactional]
         * annotation instead.
         *
         * This also enables the exception translation, which can convert any [SQLException] thrown by JDBC to
         * Spring's [DataAccessException] and rethrow it.
         *
         * @param dataSource the data source used to obtain SQL connections.
         * @param dialect the dialect implementation, by default, [StandardDialect] is used.
         * @param logger the logger used to output logs, printed to the console by default, null to disable logging.
         * @return the new-created database object.
         */
        fun connectWithSpringSupport(
            dataSource: DataSource,
            dialect: SqlDialect = StandardDialect,
            logger: Logger? = detectLoggerImplementation()
        ): Database {
            val transactionManager = SpringManagedTransactionManager(dataSource)
            val exceptionTranslator = SQLErrorCodeSQLExceptionTranslator(dataSource)
            return Database(transactionManager, dialect, logger) { exceptionTranslator.translate("Ktorm", null, it) }
        }
    }
}
