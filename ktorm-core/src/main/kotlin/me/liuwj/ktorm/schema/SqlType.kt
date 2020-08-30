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

package me.liuwj.ktorm.schema

import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.*

/**
 * Abstraction of SQL data types.
 *
 * Based on JDBC, [SqlType] and its subclasses encapsulate the common operations of obtaining data from a [ResultSet]
 * and setting parameters to a [PreparedStatement].
 *
 * @property typeCode a constant value defined in [java.sql.Types] to identify JDBC types.
 * @property typeName the name of the type in specific databases, such as `int`, `bigint`, `varchar`, etc.
 */
public abstract class SqlType<T : Any>(public val typeCode: Int, public val typeName: String) {

    /**
     * Set the [parameter] to a given [PreparedStatement], the parameter can't be null.
     */
    protected abstract fun doSetParameter(ps: PreparedStatement, index: Int, parameter: T)

    /**
     * Obtain a result from a given [ResultSet] by [index], the result may be null.
     */
    protected abstract fun doGetResult(rs: ResultSet, index: Int): T?

    /**
     * Set the nullable [parameter] to a given [PreparedStatement].
     */
    public open fun setParameter(ps: PreparedStatement, index: Int, parameter: T?) {
        if (parameter == null) {
            ps.setNull(index, typeCode)
        } else {
            doSetParameter(ps, index, parameter)
        }
    }

    /**
     * Obtain a result from a given [ResultSet] by [index].
     */
    public open fun getResult(rs: ResultSet, index: Int): T? {
        val result = doGetResult(rs, index)
        return if (rs.wasNull()) null else result
    }

    /**
     * Obtain a result from a given [ResultSet] by [columnLabel].
     */
    public open fun getResult(rs: ResultSet, columnLabel: String): T? {
        return getResult(rs, rs.findColumn(columnLabel))
    }

    /**
     * Transform this [SqlType] to another. The returned [SqlType] has the same [typeCode] and [typeName] as the
     * underlying one, and performs the specific transformations on column values.
     *
     * This function enables a user-friendly syntax to extend more data types. For example, the following code defines
     * a column of type `Column<UserRole>`, based on the existing [IntSqlType]:
     *
     * ```kotlin
     * val role by registerColumn("role", IntSqlType.transform({ UserRole.fromCode(it) }, { it.code }))
     * ```
     *
     * @param fromUnderlyingValue a function that transforms a value of underlying type to the user's type.
     * @param toUnderlyingValue a function that transforms a value of user's type the to the underlying type.
     * @return a [SqlType] instance based on this underlying type with specific transformations.
     */
    public open fun <R : Any> transform(fromUnderlyingValue: (T) -> R, toUnderlyingValue: (R) -> T): SqlType<R> {
        return object : SqlType<R>(typeCode, typeName) {
            val underlyingType = this@SqlType

            override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: R) {
                underlyingType.doSetParameter(ps, index, toUnderlyingValue(parameter))
            }

            override fun doGetResult(rs: ResultSet, index: Int): R? {
                return underlyingType.doGetResult(rs, index)?.let(fromUnderlyingValue)
            }
        }
    }

    /**
     * Indicates whether some other object is "equal to" this SQL type.
     * Two SQL types are equal if they have the same type codes and names.
     */
    override fun equals(other: Any?): Boolean {
        return other is SqlType<*> && this.typeCode == other.typeCode && this.typeName == other.typeName
    }

    /**
     * Return a hash code value for this SQL type.
     */
    override fun hashCode(): Int {
        return Objects.hash(typeCode, typeName)
    }
}
