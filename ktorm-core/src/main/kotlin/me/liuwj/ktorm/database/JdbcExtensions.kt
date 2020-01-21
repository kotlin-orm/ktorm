/*
 * Copyright 2018-2020 the original author or authors.
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

import me.liuwj.ktorm.expression.ArgumentExpression
import me.liuwj.ktorm.schema.SqlType
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

/**
 * Obtain a connection from [Database.global] and invoke the callback function with it.
 *
 * If the current thread has opened a transaction, then this transaction's connection will be used.
 * Otherwise, Ktorm will pass a new-created connection to the function and auto close it after it's
 * not useful anymore.
 *
 * @see Database.useConnection
 */
@Suppress("DEPRECATION")
@Deprecated(
    message = "This function will be removed in the future. Please use database.useConnection {...} instead.",
    replaceWith = ReplaceWith("database.useConnection(func)")
)
inline fun <T> useConnection(func: (Connection) -> T): T {
    return Database.global.useConnection(func)
}

/**
 * Execute the specific callback function in a transaction of [Database.global] and returns its result if the
 * execution succeeds, otherwise, if the execution fails, the transaction will be rollback.
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
 * @see Database.useTransaction
 */
@Suppress("DEPRECATION")
@Deprecated(
    message = "This function will be removed in the future. Please use database.useTransaction {...} instead.",
    replaceWith = ReplaceWith("database.useTransaction(isolation, func)")
)
inline fun <T> useTransaction(
    isolation: TransactionIsolation = TransactionIsolation.REPEATABLE_READ,
    func: (Transaction) -> T
): T {
    return Database.global.useTransaction(isolation, func)
}

/**
 * Execute the given [block] function on this resource and then close it down correctly whether an exception
 * is thrown or not.
 *
 * @param block a function to process this [AutoCloseable] resource.
 * @return the result of [block] function invoked on this resource.
 */
@Suppress("ConvertTryFinallyToUseCall")
inline fun <T : AutoCloseable?, R> T.use(block: (T) -> R): R {
    try {
        return block(this)
    } finally {
        this?.close()
    }
}

/**
 * Set the arguments for this [PreparedStatement].
 *
 * @param args the arguments to set into the statement.
 */
fun PreparedStatement.setArguments(args: List<ArgumentExpression<*>>) {
    for ((i, expr) in args.withIndex()) {
        @Suppress("UNCHECKED_CAST")
        val sqlType = expr.sqlType as SqlType<Any>
        sqlType.setParameter(this, i + 1, expr.value)
    }
}

/**
 * Return an iterator over the rows of this [ResultSet].
 *
 * The returned iterator just wraps the [ResultSet.next] method and every element returned by the iterator is
 * exactly the same reference as the this [ResultSet].
 */
@Suppress("IteratorHasNextCallsNextMethod")
operator fun <T : ResultSet> T.iterator() = object : Iterator<T> {
    private val rs = this@iterator
    private var hasNext: Boolean? = null

    override fun hasNext(): Boolean {
        return hasNext ?: rs.next().also { hasNext = it }
    }

    override fun next(): T {
        return if (hasNext()) rs.also { hasNext = null } else throw NoSuchElementException()
    }
}

/**
 * Wrap this [ResultSet] as [Iterable].
 *
 * This function is useful when we want to iterate a result set by a for-each loop, or process it via extension
 * functions of Kotlin standard lib, such as [Iterable.map], [Iterable.filter], etc.
 *
 * @see ResultSet.iterator
 */
fun <T : ResultSet> T.iterable(): Iterable<T> {
    return Iterable { iterator() }
}
