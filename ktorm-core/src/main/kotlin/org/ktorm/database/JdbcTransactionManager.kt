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

import java.sql.Connection
import java.sql.DriverManager
import javax.sql.DataSource

/**
 * [TransactionManager] implementation based on JDBC.
 *
 * This class is capable of working in any environment with any JDBC driver. It accepts a [connector]
 * function used to obtain SQL connections. Applications should return a native JDBC connection in
 * the callback function, no matter it's newly established by [DriverManager] directly, or obtained
 * from a connection pool such as [DataSource].
 *
 * [Database] instances created by [Database.connect] functions use this implementation by default.
 *
 * @property connector the callback function used to obtain SQL connections.
 */
public class JdbcTransactionManager(public val connector: () -> Connection) : TransactionManager {
    private val threadLocal = ThreadLocal<Transaction>()

    override val defaultIsolation: TransactionIsolation? = null

    override val currentTransaction: Transaction? get() = threadLocal.get()

    override fun newTransaction(isolation: TransactionIsolation?): Transaction {
        if (currentTransaction != null) {
            throw IllegalStateException("Current thread is already in a transaction.")
        }

        return JdbcTransaction(isolation).apply { threadLocal.set(this) }
    }

    override fun newConnection(): Connection {
        return connector.invoke()
    }

    private inner class JdbcTransaction(private val desiredIsolation: TransactionIsolation?) : Transaction {
        private var originIsolation = -1
        private var originAutoCommit = true

        private val connectionLazy = lazy(LazyThreadSafetyMode.NONE) {
            newConnection().apply {
                try {
                    if (desiredIsolation != null) {
                        originIsolation = transactionIsolation
                        if (originIsolation != desiredIsolation.level) {
                            transactionIsolation = desiredIsolation.level
                        }
                    }

                    originAutoCommit = autoCommit
                    if (originAutoCommit) {
                        autoCommit = false
                    }
                } catch (e: Throwable) {
                    closeSilently()
                    throw e
                }
            }
        }

        override val connection: Connection by connectionLazy

        override fun commit() {
            if (connectionLazy.isInitialized()) {
                connection.commit()
            }
        }

        override fun rollback() {
            if (connectionLazy.isInitialized()) {
                connection.rollback()
            }
        }

        override fun close() {
            try {
                if (connectionLazy.isInitialized() && !connection.isClosed) {
                    connection.closeSilently()
                }
            } finally {
                threadLocal.remove()
            }
        }

        @Suppress("SwallowedException")
        private fun Connection.closeSilently() {
            try {
                if (desiredIsolation != null && originIsolation != desiredIsolation.level) {
                    transactionIsolation = originIsolation
                }
                if (originAutoCommit) {
                    autoCommit = true
                }
            } catch (_: Throwable) {
            } finally {
                try {
                    close()
                } catch (_: Throwable) {
                }
            }
        }
    }
}
