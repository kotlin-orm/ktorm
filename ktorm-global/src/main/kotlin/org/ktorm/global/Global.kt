/*
 * Copyright 2018-2023 the original author or authors.
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

@file:Suppress("DEPRECATION")

package org.ktorm.global

import org.ktorm.database.*
import org.ktorm.logging.Logger
import org.ktorm.logging.detectLoggerImplementation
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator
import org.springframework.transaction.annotation.Transactional
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import javax.sql.DataSource
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@PublishedApi
internal val lastConnected: AtomicReference<Database> = AtomicReference()

@PublishedApi
internal val threadLocal: ThreadLocal<Database> = ThreadLocal()

/**
 * The global database instance, Ktorm uses this property to obtain a database when any SQL is executed.
 *
 * By default, it's the lasted connected one, but it may change if the [invoke] operator is used.
 *
 * Note that you must connect to the database via [Database.Companion.connectGlobally] first, otherwise an
 * exception will be thrown.
 *
 * @see Database.invoke
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public val Database.Companion.global: Database get() {
    val database = threadLocal.get() ?: lastConnected.get()
    return database ?: error("Not connected to any database yet, please connect to one via Database.connectGlobally")
}

/**
 * Connect to a database by a specific [connector] function and save the returned database instance
 * to [Database.Companion.global].
 *
 * @param dialect the dialect, auto-detects an implementation by default using JDK [ServiceLoader] facility.
 * @param logger logger used to output logs, auto-detects an implementation by default.
 * @param alwaysQuoteIdentifiers whether we need to always quote SQL identifiers in the generated SQLs.
 * @param generateSqlInUpperCase whether we need to output the generated SQLs in upper case.
 * @param connector the connector function used to obtain SQL connections.
 * @return the new-created database object.
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public fun Database.Companion.connectGlobally(
    dialect: SqlDialect = detectDialectImplementation(),
    logger: Logger = detectLoggerImplementation(),
    alwaysQuoteIdentifiers: Boolean = false,
    generateSqlInUpperCase: Boolean? = null,
    connector: () -> Connection
): Database {
    val database = Database(
        transactionManager = JdbcTransactionManager(connector),
        dialect = dialect,
        logger = logger,
        alwaysQuoteIdentifiers = alwaysQuoteIdentifiers,
        generateSqlInUpperCase = generateSqlInUpperCase
    )

    lastConnected.set(database)
    return database
}

/**
 * Connect to a database using a [DataSource] and save the returned database instance to [Database.Companion.global].
 *
 * @param dataSource the data source used to obtain SQL connections.
 * @param dialect the dialect, auto-detects an implementation by default using JDK [ServiceLoader] facility.
 * @param logger logger used to output logs, auto-detects an implementation by default.
 * @param alwaysQuoteIdentifiers whether we need to always quote SQL identifiers in the generated SQLs.
 * @param generateSqlInUpperCase whether we need to output the generated SQLs in upper case.
 * @return the new-created database object.
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public fun Database.Companion.connectGlobally(
    dataSource: DataSource,
    dialect: SqlDialect = detectDialectImplementation(),
    logger: Logger = detectLoggerImplementation(),
    alwaysQuoteIdentifiers: Boolean = false,
    generateSqlInUpperCase: Boolean? = null
): Database {
    val database = Database(
        transactionManager = JdbcTransactionManager { dataSource.connection },
        dialect = dialect,
        logger = logger,
        alwaysQuoteIdentifiers = alwaysQuoteIdentifiers,
        generateSqlInUpperCase = generateSqlInUpperCase
    )

    lastConnected.set(database)
    return database
}

/**
 * Connect to a database using the specific connection arguments and save the returned database instance
 * to [Database.Companion.global].
 *
 * @param url the URL of the database to be connected.
 * @param driver the full qualified name of the JDBC driver class.
 * @param user the username of the database.
 * @param password the password of the database.
 * @param dialect the dialect, auto-detects an implementation by default using JDK [ServiceLoader] facility.
 * @param logger logger used to output logs, auto-detects an implementation by default.
 * @param alwaysQuoteIdentifiers whether we need to always quote SQL identifiers in the generated SQLs.
 * @param generateSqlInUpperCase whether we need to output the generated SQLs in upper case.
 * @return the new-created database object.
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public fun Database.Companion.connectGlobally(
    url: String,
    driver: String? = null,
    user: String? = null,
    password: String? = null,
    dialect: SqlDialect = detectDialectImplementation(),
    logger: Logger = detectLoggerImplementation(),
    alwaysQuoteIdentifiers: Boolean = false,
    generateSqlInUpperCase: Boolean? = null
): Database {
    if (!driver.isNullOrBlank()) {
        Class.forName(driver)
    }

    val database = Database(
        transactionManager = JdbcTransactionManager { DriverManager.getConnection(url, user, password) },
        dialect = dialect,
        logger = logger,
        alwaysQuoteIdentifiers = alwaysQuoteIdentifiers,
        generateSqlInUpperCase = generateSqlInUpperCase
    )

    lastConnected.set(database)
    return database
}

/**
 * Connect to a database using a [DataSource] with the Spring support enabled and save the returned database
 * instance to [Database.Companion.global].
 *
 * Once the Spring support is enabled, the transaction management will be delegated to the Spring framework,
 * so the [useTransaction] function is not available anymore, we need to use Spring's [Transactional]
 * annotation instead.
 *
 * This function also enables the exception translation, which can convert any [SQLException] thrown by JDBC
 * to Spring's [DataAccessException] and rethrow it.
 *
 * @param dataSource the data source used to obtain SQL connections.
 * @param dialect the dialect, auto-detects an implementation by default using JDK [ServiceLoader] facility.
 * @param logger logger used to output logs, auto-detects an implementation by default.
 * @param alwaysQuoteIdentifiers whether we need to always quote SQL identifiers in the generated SQLs.
 * @param generateSqlInUpperCase whether we need to output the generated SQLs in upper case.
 * @return the new-created database object.
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public fun Database.Companion.connectWithSpringSupportGlobally(
    dataSource: DataSource,
    dialect: SqlDialect = detectDialectImplementation(),
    logger: Logger = detectLoggerImplementation(),
    alwaysQuoteIdentifiers: Boolean = false,
    generateSqlInUpperCase: Boolean? = null
): Database {
    val translator = SQLErrorCodeSQLExceptionTranslator(dataSource)

    val database = Database(
        transactionManager = SpringManagedTransactionManager(dataSource),
        dialect = dialect,
        logger = logger,
        exceptionTranslator = { ex -> translator.translate("Ktorm", null, ex) },
        alwaysQuoteIdentifiers = alwaysQuoteIdentifiers,
        generateSqlInUpperCase = generateSqlInUpperCase
    )

    lastConnected.set(database)
    return database
}

/**
 * Execute the callback function using the current database instance.
 *
 * Useful when we have many database instances. Call this function to choose one to execute
 * our database specific operations. While the callback functions are executing, the [Database.Companion.global]
 * property will be set to the current database. And after the callback completes, it's automatically
 * restored to the origin one.
 *
 * @see Database.Companion.global
 */
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public inline operator fun <T> Database.invoke(func: Database.() -> T): T {
    // Contracts are not allowed for operator functions?
    // contract {
    //     callsInPlace(func, InvocationKind.EXACTLY_ONCE)
    // }

    val origin = threadLocal.get()

    try {
        threadLocal.set(this)
        return this.func()
    } catch (e: SQLException) {
        throw exceptionTranslator?.invoke(e) ?: e
    } finally {
        origin?.let { threadLocal.set(it) } ?: threadLocal.remove()
    }
}

/**
 * Obtain a connection from [Database.Companion.global] and invoke the callback function with it.
 *
 * If the current thread has opened a transaction, then this transaction's connection will be used.
 * Otherwise, Ktorm will pass a new-created connection to the function and auto close it after it's
 * not useful anymore.
 *
 * @see Database.useConnection
 */
@OptIn(ExperimentalContracts::class)
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public inline fun <T> useConnection(func: (Connection) -> T): T {
    contract {
        callsInPlace(func, InvocationKind.EXACTLY_ONCE)
    }

    return Database.global.useConnection(func)
}

/**
 * Execute the specific callback function in a transaction of [Database.Companion.global] and returns its result if the
 * execution succeeds, otherwise, if the execution fails, the transaction will be rollback.
 *
 * Note:
 *
 * - Any exceptions thrown in the callback function can trigger a rollback.
 * - This function is reentrant, so it can be called nested. However, the inner calls don’t open new transactions
 * but share the same ones with outers.
 * - Since version 3.3.0, the default isolation has changed to null (stands for the default isolation level of the
 * underlying datastore), not [TransactionIsolation.REPEATABLE_READ] anymore.
 *
 * @param isolation transaction isolation, null for the default isolation level of the underlying datastore.
 * @param func the executed callback function.
 * @return the result of the callback function.
 * @see Database.useTransaction
 */
@OptIn(ExperimentalContracts::class)
@Deprecated("ktorm-global will be removed in the future, please migrate to the standard API.")
public inline fun <T> useTransaction(isolation: TransactionIsolation? = null, func: (Transaction) -> T): T {
    contract {
        callsInPlace(func, InvocationKind.EXACTLY_ONCE)
    }

    return Database.global.useTransaction(isolation, func)
}
