package me.liuwj.ktorm.database

import me.liuwj.ktorm.expression.ArgumentExpression
import me.liuwj.ktorm.expression.SqlExpression
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.DriverManager
import java.sql.SQLException
import java.util.concurrent.atomic.AtomicReference
import javax.sql.DataSource

/**
 * Ktorm 入口类，用于连接数据库，统一管理连接以及事务
 *
 * @property transactionManager 事务管理器
 * @property dialect 数据库方言
 * @property logger 需要使用的日志实现，默认为 null，不输出任何日志
 * @property exceptionTranslator 转换 SQL 执行过程中产生的异常，以便重新抛出，符合其他框架（如 Spring JDBC）的异常标准
 */
class Database(
    val transactionManager: TransactionManager,
    val dialect: SqlDialect = StandardDialect,
    val logger: Logger? = null,
    val exceptionTranslator: (SQLException) -> Throwable = { it }
) {
    init {
        lastConnected.set(this)
    }

    /**
     * 数据库连接 URL
     */
    val url: String by lazy { useMetadata { it.url } }

    /**
     * 所连接的数据库名称
     */
    val name: String get() = url.substringAfterLast('/').substringBefore('?')

    /**
     * 数据库产品名称，如 MySQL
     */
    val productName: String by lazy { useMetadata { it.databaseProductName } }

    /**
     * 数据库版本号
     */
    val productVersion: String by lazy { useMetadata { it.databaseProductVersion } }

    /**
     * SQL 关键字集合（大写）
     */
    val keywords: Set<String> by lazy { useMetadata { ANSI_SQL_2003_KEYWORDS + it.sqlKeywords.toUpperCase().split(',') } }

    /**
     * 用于括住特殊 SQL 标识符的字符串，如 `un-standard identifier`
     */
    val identifierQuoteString: String by lazy { useMetadata { it.identifierQuoteString.trim() } }

    /**
     * 可在 SQL 标识符中使用的字符（除字母、数字、下划线以外）
     */
    val extraNameCharacters: String by lazy { useMetadata { it.extraNameCharacters } }

    /**
     * 在回调函数中使用数据库元数据，函数调用后，会自动关闭 metadata
     */
    inline fun <T> useMetadata(func: (DatabaseMetaData) -> T): T {
        useConnection { conn ->
            return func(conn.metaData)
        }
    }

    /**
     * 获取或创建数据库连接，在回调函数中使用，使用完毕后，会自动回收连接
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
     * 在事务中执行指定函数
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
     * 将回调函数范围内的全局数据库对象设置为当前对象，函数结束后，恢复原样
     *
     * @see Database.global
     */
    operator fun <T> invoke(block: Database.() -> T): T {
        val origin = threadLocal.get()

        try {
            threadLocal.set(this)
            return this.block()

        } catch (e: SQLException) {
            throw exceptionTranslator.invoke(e)

        } finally {
            origin?.let { threadLocal.set(it) } ?: threadLocal.remove()
        }
    }

    /**
     * 将表达式格式化为可直接执行的 SQL 字符串
     *
     * @param expression 需要格式化的表达式
     * @param beautifySql 是否需要换行、缩进
     * @param indentSize 缩进长度
     * @return SQL 字符串
     * @return 该 SQL 的所有参数数据
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

    companion object {
        private val lastConnected = AtomicReference<Database>()
        private val threadLocal = ThreadLocal<Database>()

        /**
         * 获取应用程序连接的全局数据库对象
         */
        val global: Database get() = threadLocal.get() ?: lastConnected.get() ?: error("Not connected to any database yet.")

        /**
         * 使用原生 JDBC 连接数据库，在回调函数中获取数据库连接
         */
        fun connect(
            dialect: SqlDialect = StandardDialect,
            logger: Logger? = null,
            connector: () -> Connection
        ): Database {
            return Database(JdbcTransactionManager(connector), dialect, logger)
        }

        /**
         * 使用原生 JDBC 连接数据库，在参数提供的数据源中获取连接
         */
        fun connect(
            dataSource: DataSource,
            dialect: SqlDialect = StandardDialect,
            logger: Logger? = null
        ): Database {
            return connect(dialect, logger) { dataSource.connection }
        }

        /**
         * 使用原生 JDBC 连接数据库，参数提供数据库 url、驱动类名、用户名、密码
         */
        fun connect(
            url: String,
            driver: String,
            user: String = "",
            password: String = "",
            dialect: SqlDialect = StandardDialect,
            logger: Logger? = null
        ): Database {
            Class.forName(driver)
            return connect(dialect, logger) { DriverManager.getConnection(url, user, password) }
        }

        /**
         * 使用 Spring 管理的数据源连接数据库
         */
        fun connectWithSpringSupport(
            dataSource: DataSource,
            dialect: SqlDialect = StandardDialect,
            logger: Logger? = null
        ): Database {
            val transactionManager = SpringManagedTransactionManager(dataSource)
            val exceptionTranslator = SQLErrorCodeSQLExceptionTranslator(dataSource)
            return Database(transactionManager, dialect, logger) { exceptionTranslator.translate("Ktorm", null, it) }
        }
    }
}