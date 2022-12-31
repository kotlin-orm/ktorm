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

package org.ktorm.database

import org.springframework.transaction.annotation.Transactional
import java.io.Closeable
import java.sql.Connection

/**
 * Transaction manager abstraction used to manage database connections and transactions.
 *
 * Applications can use this interface directly, but it is not primary meant as API:
 * Typically, transactions are used by calling the [Database.useTransaction] function or
 * Spring's [Transactional] annotation if the Spring support is enabled.
 */
public interface TransactionManager {

    /**
     * The default transaction isolation, null for the default isolation level of the underlying datastore.
     */
    public val defaultIsolation: TransactionIsolation?

    /**
     * The opened transaction of the current thread, null if there is no transaction opened.
     */
    public val currentTransaction: Transaction?

    /**
     * Open a new transaction for the current thread using the specific isolation if there is no transaction opened.
     *
     * @param isolation the transaction isolation, by default, [defaultIsolation] is used.
     * @return the new-created transaction.
     * @throws [IllegalStateException] if there is already a transaction opened.
     */
    public fun newTransaction(isolation: TransactionIsolation? = defaultIsolation): Transaction

    /**
     * Create a native JDBC connection to the database.
     */
    public fun newConnection(): Connection
}

/**
 * Representation of a transaction.
 *
 * Transactional code can use this interface to retrieve the backend connection, and
 * to programmatically trigger a commit or rollback (instead of implicit commits and rollbacks
 * of using [Database.useTransaction]).
 */
public interface Transaction : Closeable {

    /**
     * The backend JDBC connection of this transaction.
     */
    public val connection: Connection

    /**
     * Commit the transaction.
     */
    public fun commit()

    /**
     * Rollback the transaction.
     */
    public fun rollback()

    /**
     * Close the transaction and release its underlying resources (e.g. the backend connection).
     */
    override fun close()
}

/**
 * Enum class represents transaction isolation levels, wrapping the `TRANSACTION_XXX` constants
 * defined in the [Connection] interface.
 *
 * @property level the `TRANSACTION_XXX` constant values defined in the [Connection] interface.
 */
public enum class TransactionIsolation(public val level: Int) {
    NONE(Connection.TRANSACTION_NONE),
    READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED),
    READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED),
    REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ),
    SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE);

    public companion object {

        /**
         * Find an enum value by the specific isolation level.
         */
        public fun valueOf(level: Int): TransactionIsolation {
            return values().first { it.level == level }
        }
    }
}
