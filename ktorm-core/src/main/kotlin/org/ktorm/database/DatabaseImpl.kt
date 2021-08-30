/*
 * Copyright 2018-2021 the original author or authors.
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

import org.ktorm.dsl.Query
import org.ktorm.entity.EntitySequence
import org.ktorm.expression.ArgumentExpression
import org.ktorm.expression.SqlExpression
import org.ktorm.logging.Logger
import org.ktorm.logging.detectLoggerImplementation
import java.sql.*
import java.util.*
import javax.sql.DataSource
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * The entry class of Ktorm, represents a physical database, used to manage connections and transactions.
 *
 * ### Connect with a URL
 *
 * The simplest way to create a database instance, using a JDBC URL:
 *
 * ```kotlin
 * val database = Database.connect("jdbc:mysql://localhost:3306/ktorm", user = "root", password = "123")
 * ```
 *
 * Easy to know what we do in the [connect] function. Just like any JDBC boilerplate code, Ktorm loads the MySQL
 * database driver first, then calls [DriverManager.getConnection] with your URL to obtain a connection.
 *
 * Of course, Ktorm doesn't call [DriverManager.getConnection] in the beginning. Instead, we obtain connections
 * only when it's really needed (such as executing a SQL), then close them after they are not useful anymore.
 * Therefore, database objects created by this way won't reuse any connections, creating connections frequently
 * can lead to huge performance costs. It's highly recommended to use connection pools in your production environment.
 *
 * ### Connect with a Pool
 *
 * Ktorm doesn't limit you, you can use any connection pool you like, such as DBCP, C3P0 or Druid. The [connect]
 * function provides an overloaded version which accepts a [DataSource] parameter, you just need to create a
 * [DataSource] object and call that function with it:
 *
 * ```kotlin
 * val dataSource = SingleConnectionDataSource() // Any DataSource implementation is OK.
 * val database = Database.connect(dataSource)
 * ```
 *
 * Now, Ktorm will obtain connections from the [DataSource] when necessary, then return them to the pool after they
 * are not useful. This avoids the performance costs of frequent connection creation.
 *
 * Connection pools are applicative and effective in most cases, we highly recommend you manage your connections
 * in this way.
 *
 * ### Use SQL DSL & Sequence APIs
 *
 * Now that we've connected to the database, we can perform many operations on it. Ktorm's APIs are mainly divided
 * into two parts, they are SQL DSL and sequence APIs.
 *
 * Here, we use SQL DSL to obtains the names of all engineers in department 1:
 *
 * ```kotlin
 * database
 *     .from(Employees)
 *     .select(Employees.name)
 *     .where { (Employees.departmentId eq 1) and (Employees.job eq "engineer") }
 *     .forEach { row ->
 *         println(row[Employees.name])
 *     }
 * ```
 *
 * Equivalent code using sequence APIs:
 *
 * ```kotlin
 * database
 *     .sequenceOf(Employees)
 *     .filter { it.departmentId eq 1 }
 *     .filter { it.job eq "engineer" }
 *     .mapColumns { it.name }
 *     .forEach { name ->
 *         println(name)
 *     }
 * ```
 * More details about SQL DSL, see [Query], about sequence APIs, see [EntitySequence].
 */
public class DatabaseImpl(

    /**
     * The transaction manager used to manage connections and transactions.
     */
    override val transactionManager: TransactionManager,

    /**
     * The dialect, auto detects an implementation by default using JDK [ServiceLoader] facility.
     */
    override val dialect: SqlDialect = detectDialectImplementation(),

    /**
     * The logger used to output logs, auto detects an implementation by default.
     */
    override val logger: Logger = detectLoggerImplementation(),

    /**
     * Function used to translate SQL exceptions so as to rethrow them to users.
     */
    override val exceptionTranslator: ((SQLException) -> Throwable)? = null,

    /**
     * Whether we need to always quote SQL identifiers in the generated SQLs.
     *
     * @since 3.1.0
     */
    override val alwaysQuoteIdentifiers: Boolean = false,

    /**
     * Whether we need to output the generated SQLs in upper case.
     *
     * `true` for upper case, `false` for lower case, `null` for default (the database preferred style).
     *
     * @since 3.1.0
     */
    override val generateSqlInUpperCase: Boolean? = null
) : Database {
    /**
     * The URL of the connected database.
     */
    override val url: String

    /**
     * The name of the connected database.
     */
    override val name: String

    /**
     * The name of the connected database product, eg. MySQL, H2.
     */
    override val productName: String

    /**
     * The version of the connected database product.
     */
    override val productVersion: String

    /**
     * A set of all of this database's SQL keywords (including SQL:2003 keywords), all in uppercase.
     */
    override val keywords: Set<String>

    /**
     * The string used to quote SQL identifiers, returns an empty string if identifier quoting is not supported.
     */
    override val identifierQuoteString: String

    /**
     * All the "extra" characters that can be used in unquoted identifier names (those beyond a-z, A-Z, 0-9 and _).
     */
    override val extraNameCharacters: String

    /**
     * Whether this database treats mixed case unquoted SQL identifiers as case sensitive and as a result
     * stores them in mixed case.
     *
     * @since 3.1.0
     */
    override val supportsMixedCaseIdentifiers: Boolean

    /**
     * Whether this database treats mixed case unquoted SQL identifiers as case insensitive and
     * stores them in mixed case.
     *
     * @since 3.1.0
     */
    override val storesMixedCaseIdentifiers: Boolean

    /**
     * Whether this database treats mixed case unquoted SQL identifiers as case insensitive and
     * stores them in upper case.
     *
     * @since 3.1.0
     */
    override val storesUpperCaseIdentifiers: Boolean

    /**
     * Whether this database treats mixed case unquoted SQL identifiers as case insensitive and
     * stores them in lower case.
     *
     * @since 3.1.0
     */
    override val storesLowerCaseIdentifiers: Boolean

    /**
     * Whether this database treats mixed case quoted SQL identifiers as case sensitive and as a result
     * stores them in mixed case.
     *
     * @since 3.1.0
     */
    override val supportsMixedCaseQuotedIdentifiers: Boolean

    /**
     * Whether this database treats mixed case quoted SQL identifiers as case insensitive and
     * stores them in mixed case.
     *
     * @since 3.1.0
     */
    override val storesMixedCaseQuotedIdentifiers: Boolean

    /**
     * Whether this database treats mixed case quoted SQL identifiers as case insensitive and
     * stores them in upper case.
     *
     * @since 3.1.0
     */
    override val storesUpperCaseQuotedIdentifiers: Boolean

    /**
     * Whether this database treats mixed case quoted SQL identifiers as case insensitive and
     * stores them in lower case.
     *
     * @since 3.1.0
     */
    override val storesLowerCaseQuotedIdentifiers: Boolean

    /**
     * The maximum number of characters this database allows for a column name. Zero means that there is no limit
     * or the limit is not known.
     *
     * @since 3.1.0
     */
    override val maxColumnNameLength: Int

    init {
        fun Result<String?>.orEmpty() = getOrNull().orEmpty()
        fun Result<Boolean>.orFalse() = getOrDefault(false)

        useConnection { conn ->
            val metadata = conn.metaData
            url = metadata.runCatching { url }.orEmpty()
            name = url.substringAfterLast('/').substringBefore('?')
            productName = metadata.runCatching { databaseProductName }.orEmpty()
            productVersion = metadata.runCatching { databaseProductVersion }.orEmpty()
            keywords = ANSI_SQL_2003_KEYWORDS + metadata.runCatching { sqlKeywords }.orEmpty().toUpperCase().split(',')
            identifierQuoteString = metadata.runCatching { identifierQuoteString }.orEmpty().trim()
            extraNameCharacters = metadata.runCatching { extraNameCharacters }.orEmpty()
            supportsMixedCaseIdentifiers = metadata.runCatching { supportsMixedCaseIdentifiers() }.orFalse()
            storesMixedCaseIdentifiers = metadata.runCatching { storesMixedCaseIdentifiers() }.orFalse()
            storesUpperCaseIdentifiers = metadata.runCatching { storesUpperCaseIdentifiers() }.orFalse()
            storesLowerCaseIdentifiers = metadata.runCatching { storesLowerCaseIdentifiers() }.orFalse()
            supportsMixedCaseQuotedIdentifiers = metadata.runCatching { supportsMixedCaseQuotedIdentifiers() }.orFalse()
            storesMixedCaseQuotedIdentifiers = metadata.runCatching { storesMixedCaseQuotedIdentifiers() }.orFalse()
            storesUpperCaseQuotedIdentifiers = metadata.runCatching { storesUpperCaseQuotedIdentifiers() }.orFalse()
            storesLowerCaseQuotedIdentifiers = metadata.runCatching { storesLowerCaseQuotedIdentifiers() }.orFalse()
            maxColumnNameLength = metadata.runCatching { maxColumnNameLength }.getOrDefault(0)
        }

        if (logger.isInfoEnabled()) {
            val msg = "Connected to %s, productName: %s, productVersion: %s, logger: %s, dialect: %s"
            logger.info(msg.format(url, productName, productVersion, logger, dialect))
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
    override fun formatExpression(
        expression: SqlExpression,
        beautifySql: Boolean,
        indentSize: Int
    ): Pair<String, List<ArgumentExpression<*>>> {
        val formatter = dialect.createSqlFormatter(this, beautifySql, indentSize)
        formatter.visit(expression)
        return Pair(formatter.sql, formatter.parameters)
    }

    /**
     * Format the given [expression] to a SQL string with its execution arguments, then create
     * a [PreparedStatement] from the this database using the SQL string and execute the specific
     * callback function with it. After the callback function completes, the statement will be
     * closed automatically.
     *
     * @since 2.7
     * @param expression the SQL expression to be executed.
     * @param func the callback function.
     * @return the result of the callback function.
     */
    @OptIn(ExperimentalContracts::class)
    public inline fun <T> executeExpression(expression: SqlExpression, func: (PreparedStatement) -> T): T {
        contract {
            callsInPlace(func, InvocationKind.EXACTLY_ONCE)
        }

        val (sql, args) = formatExpression(expression)

        if (logger.isDebugEnabled()) {
            logger.debug("SQL: $sql")
            logger.debug("Parameters: " + args.map { "${it.value}(${it.sqlType.typeName})" })
        }

        useConnection { conn ->
            conn.prepareStatement(sql).use { statement ->
                statement.setArguments(args)
                return func(statement)
            }
        }
    }

    /**
     * Format the given [expression] to a SQL string with its execution arguments, then execute it via
     * [PreparedStatement.executeQuery] and return the result [CachedRowSet].
     *
     * @since 2.7
     * @param expression the SQL expression to be executed.
     * @return the result [CachedRowSet].
     */
    override fun executeQuery(expression: SqlExpression): CachedRowSet {
        executeExpression(expression) { statement ->
            statement.executeQuery().use { rs ->
                val rowSet = CachedRowSet(rs)

                if (logger.isDebugEnabled()) {
                    logger.debug("Results: ${rowSet.size()}")
                }

                return rowSet
            }
        }
    }

    /**
     * Format the given [expression] to a SQL string with its execution arguments, then execute it via
     * [PreparedStatement.executeUpdate] and return the effected row count.
     *
     * @since 2.7
     * @param expression the SQL expression to be executed.
     * @return the effected row count.
     */
    override fun executeUpdate(expression: SqlExpression): Int {
        executeExpression(expression) { statement ->
            val effects = statement.executeUpdate()

            if (logger.isDebugEnabled()) {
                logger.debug("Effects: $effects")
            }

            return effects
        }
    }

    /**
     * Format the given [expression] to a SQL string with its execution arguments, execute it via
     * [PreparedStatement.executeUpdate], then return the effected row count along with the generated keys.
     *
     * @since 2.7
     * @param expression the SQL expression to be executed.
     * @return a [Pair] combines the effected row count and the generated keys.
     */
    override fun executeUpdateAndRetrieveKeys(expression: SqlExpression): Pair<Int, CachedRowSet> {
        val (sql, args) = formatExpression(expression)

        if (logger.isDebugEnabled()) {
            logger.debug("SQL: $sql")
            logger.debug("Parameters: " + args.map { "${it.value}(${it.sqlType.typeName})" })
        }

        val (effects, rowSet) = dialect.executeUpdateAndRetrieveKeys(this, sql, args)

        if (logger.isDebugEnabled()) {
            logger.debug("Effects: $effects")
        }

        return Pair(effects, rowSet)
    }

    /**
     * Batch execute the given SQL expressions and return the effected row counts for each expression.
     *
     * Note that this function is implemented based on [Statement.addBatch] and [Statement.executeBatch],
     * and any item in a batch operation must have the same structure, otherwise an exception will be thrown.
     *
     * @since 2.7
     * @param expressions the SQL expressions to be executed.
     * @return the effected row counts for each sub-operation.
     */
    override fun executeBatch(expressions: List<SqlExpression>): IntArray {
        val (sql, _) = formatExpression(expressions[0])

        if (logger.isDebugEnabled()) {
            logger.debug("SQL: $sql")
        }

        useConnection { conn ->
            conn.prepareStatement(sql).use { statement ->
                for (expr in expressions) {
                    val (subSql, args) = formatExpression(expr)

                    if (subSql != sql) {
                        throw IllegalArgumentException(
                            "Every item in a batch operation must generate the same SQL: \n\n$subSql"
                        )
                    }
                    if (logger.isDebugEnabled()) {
                        logger.debug("Parameters: " + args.map { "${it.value}(${it.sqlType.typeName})" })
                    }

                    statement.setArguments(args)
                    statement.addBatch()
                }

                val effects = statement.executeBatch()

                if (logger.isDebugEnabled()) {
                    logger.debug("Effects: ${effects?.contentToString()}")
                }

                return effects
            }
        }
    }
}
