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

package org.ktorm.schema

import java.math.BigDecimal
import java.sql.*
import java.sql.Date
import java.time.*
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
     * @param toUnderlyingValue a function that transforms a value of user's type to the underlying type.
     * @return a [SqlType] instance based on this underlying type with specific transformations.
     */
    public open fun <R : Any> transform(fromUnderlyingValue: (T) -> R, toUnderlyingValue: (R) -> T): SqlType<R> {
        return object : SqlType<R>(typeCode, typeName) {
            val underlyingType = this@SqlType

            override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: R) {
                underlyingType.doSetParameter(ps, index, toUnderlyingValue(parameter))
            }

            override fun doGetResult(rs: ResultSet, index: Int): R? {
                val result = underlyingType.doGetResult(rs, index)
                return if (rs.wasNull()) null else fromUnderlyingValue(result!!)
            }
        }
    }

    /**
     * Indicates whether some other object is "equal to" this SQL type.
     * Two SQL types are equal if they have the same type codes and names.
     */
    override fun equals(other: Any?): Boolean {
        return other is SqlType<*>
            && this::class == other::class
            && this.typeCode == other.typeCode
            && this.typeName == other.typeName
    }

    /**
     * Return a hash code value for this SQL type.
     */
    override fun hashCode(): Int {
        return Objects.hash(typeCode, typeName)
    }

    /**
     * Companion object provides some utility functions.
     */
    public companion object {

        /**
         * Return the corresponding ktorm core built-in [SqlType] for kotlin type [T].
         */
        @Suppress("UNCHECKED_CAST")
        public inline fun <reified T : Any> of(): SqlType<T>? {
            val kotlinType = T::class
            if (kotlinType.java.isEnum) {
                return EnumSqlType(kotlinType.java as Class<out Enum<*>>) as SqlType<T>
            }

            val sqlType = when (kotlinType) {
                Boolean::class -> BooleanSqlType
                Int::class -> IntSqlType
                Short::class -> ShortSqlType
                Long::class -> LongSqlType
                Float::class -> FloatSqlType
                Double::class -> DoubleSqlType
                BigDecimal::class -> DecimalSqlType
                String::class -> VarcharSqlType
                ByteArray::class -> BytesSqlType
                Timestamp::class -> TimestampSqlType
                Date::class -> DateSqlType
                Time::class -> TimeSqlType
                Instant::class -> InstantSqlType
                LocalDateTime::class -> LocalDateTimeSqlType
                LocalDate::class -> LocalDateSqlType
                LocalTime::class -> LocalTimeSqlType
                MonthDay::class -> MonthDaySqlType
                YearMonth::class -> YearMonthSqlType
                Year::class -> YearSqlType
                UUID::class -> UuidSqlType
                else -> null
            }

            return sqlType as SqlType<T>?
        }
    }
}
