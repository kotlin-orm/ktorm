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
import java.sql.PreparedStatement
import java.sql.ResultSet
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Execute the given [block] function on this resource and then close it down correctly whether an exception
 * is thrown or not.
 *
 * @param block a function to process this [AutoCloseable] resource.
 * @return the result of [block] function invoked on this resource.
 */
@Suppress("ConvertTryFinallyToUseCall")
inline fun <T : AutoCloseable?, R> T.use(block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    try {
        return block(this)
    } finally {
        this?.close()
    }
}

/**
 * Set the arguments for this [PreparedStatement].
 *
 * @since 2.7
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
 * functions of Kotlin standard lib, such as [Iterable.map], [Iterable.flatMap], etc.
 *
 * @see ResultSet.iterator
 */
@Deprecated(
    message = "This function will be removed in the future. Please use asIterable() instead.",
    replaceWith = ReplaceWith("asIterable()")
)
fun <T : ResultSet> T.iterable(): Iterable<T> {
    return Iterable { iterator() }
}

/**
 * Wrap this [ResultSet] as [Iterable].
 *
 * This function is useful when we want to iterate a result set by a for-each loop, or process it via extension
 * functions of Kotlin standard lib, such as [Iterable.map], [Iterable.flatMap], etc.
 *
 * @see ResultSet.iterator
 */
fun <T : ResultSet> T.asIterable(): Iterable<T> {
    return Iterable { iterator() }
}
