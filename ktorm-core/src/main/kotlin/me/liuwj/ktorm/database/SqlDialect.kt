/*
 * Copyright 2018-2019 the original author or authors.
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

import me.liuwj.ktorm.expression.SqlFormatter

/**
 * Representation of a SQL dialect.
 *
 * It's known that there is a uniform standard for SQL language, but beyond the standard,
 * many databases still have their special features. The interface provides an extension mechanism
 * for Ktorm and its extension modules to support those dialect-specific SQL features.
 *
 * Implementations of this interface are recommended to be published as separated modules
 * independent of ktorm-core.
 *
 * To enable a dialect, applications should add the dialect module to the classpath first, then
 * configure the [Database.dialect] property to the dialect implementation while creating database
 * instances via [Database.connect] functions.
 */
interface SqlDialect {

    /**
     * Create a [SqlFormatter] instance, formatting SQL expressions as strings with their execution arguments.
     *
     * @param database the current database instance executing the formatted SQL.
     * @param beautifySql if we should output beautiful SQL strings with line-wrapping and indentation.
     * @param indentSize the indent size.
     * @return a [SqlFormatter] object, generally typed of subclasses to support dialect-specific sql expressions.
     */
    fun createSqlFormatter(database: Database, beautifySql: Boolean, indentSize: Int): SqlFormatter
}

/**
 * [SqlDialect] implementation for standard SQL, doesn't support any dialect features.
 */
object StandardDialect : SqlDialect {

    override fun createSqlFormatter(database: Database, beautifySql: Boolean, indentSize: Int): SqlFormatter {
        return object : SqlFormatter(database, beautifySql, indentSize) { }
    }
}

/**
 * Thrown to indicate that a feature is not supported by the current dialect.
 *
 * @param message the detail message, which is saved for later retrieval by [Throwable.message].
 * @param cause the cause, which is saved for later retrieval by [Throwable.cause].
 */
class DialectFeatureNotSupportedException(
    message: String? = null,
    cause: Throwable? = null
) : UnsupportedOperationException(message, cause) {

    companion object {
        private const val serialVersionUID = 1L
    }
}
