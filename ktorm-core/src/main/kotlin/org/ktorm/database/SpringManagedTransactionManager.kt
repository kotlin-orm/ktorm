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

import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
import org.springframework.transaction.annotation.Transactional
import java.sql.Connection
import javax.sql.DataSource

/**
 * [TransactionManager] implementation that delegates all transactions to the Spring framework.
 *
 * This class enables the Spring support, and it's used by [Database] instances created
 * by [Database.connectWithSpringSupport] function. Once the Spring support enabled, the
 * transaction management will be delegated to the Spring framework, so the [Database.useTransaction]
 * function is not available anymore, applications should use Spring's [Transactional] annotation instead.
 *
 * @property dataSource the data source used to obtained connections, typically comes from Spring's application context.
 */
public class SpringManagedTransactionManager(public val dataSource: DataSource) : TransactionManager {
    private val proxy = dataSource as? TransactionAwareDataSourceProxy ?: TransactionAwareDataSourceProxy(dataSource)

    override val defaultIsolation: TransactionIsolation? = null

    override val currentTransaction: Transaction? = null

    override fun newTransaction(isolation: TransactionIsolation?): Nothing {
        val msg = "Transaction is managed by Spring, please use Spring's @Transactional annotation instead."
        throw UnsupportedOperationException(msg)
    }

    override fun newConnection(): Connection {
        return proxy.connection
    }
}
