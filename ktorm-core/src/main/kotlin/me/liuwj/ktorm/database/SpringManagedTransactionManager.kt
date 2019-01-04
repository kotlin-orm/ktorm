package me.liuwj.ktorm.database

import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
import java.sql.Connection
import javax.sql.DataSource

/**
 * Created by vince on Sep 24, 2018.
 */
class SpringManagedTransactionManager(val dataSource: DataSource) : TransactionManager {

    val dataSourceProxy = dataSource as? TransactionAwareDataSourceProxy ?: TransactionAwareDataSourceProxy(dataSource)

    override val defaultIsolation get() = TransactionIsolation.REPEATABLE_READ

    override val currentTransaction: Transaction? = null

    override fun newTransaction(isolation: TransactionIsolation): Nothing {
        val msg = "Transaction is managed by Spring, please use Spring's @Transactional annotation instead."
        throw UnsupportedOperationException(msg)
    }

    override fun newConnection(): Connection {
        return dataSourceProxy.connection
    }
}