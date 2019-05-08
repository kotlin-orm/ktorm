package me.liuwj.ktorm.database

import java.io.Closeable
import java.sql.Connection

/**
 * 事务管理器
 */
interface TransactionManager {

    /**
     * 默认事务隔离级别
     */
    val defaultIsolation: TransactionIsolation

    /**
     * 获取当前线程中的事务，若尚未开启事务，返回 null
     */
    val currentTransaction: Transaction?

    /**
     * 使用指定的事务隔离级别创建一个新事务，如果当前已经开启事务，则抛出异常
     *
     * @throws [IllegalStateException] if [currentTransaction] is not null
     */
    fun newTransaction(isolation: TransactionIsolation = defaultIsolation): Transaction

    /**
     * 创建新的数据库连接
     */
    fun newConnection(): Connection
}

/**
 * 数据库事务
 */
interface Transaction : Closeable {

    /**
     * 当前事务持有的数据库连接
     */
    val connection: Connection

    /**
     * 提交事务
     */
    fun commit()

    /**
     * 回滚事务
     */
    fun rollback()

    /**
     * 关闭事务，释放底层的连接
     */
    override fun close()
}

/**
 * 事务隔离级别
 *
 * @see Connection.getTransactionIsolation
 */
enum class TransactionIsolation(val level: Int) {
    NONE(Connection.TRANSACTION_NONE),
    READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED),
    READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED),
    REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ),
    SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE);

    companion object {
        fun valueOf(level: Int): TransactionIsolation {
            return TransactionIsolation.values().first { it.level == level }
        }
    }
}
