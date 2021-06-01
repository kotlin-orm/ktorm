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

@file:Suppress("unused")

package org.ktorm.autotable

import org.ktorm.autotable.annotations.TableField
import org.ktorm.autotable.annotations.TableName
import org.ktorm.dsl.Query
import org.ktorm.dsl.QueryRowSet
import org.ktorm.dsl.map
import org.ktorm.logging.Logger
import org.ktorm.logging.detectLoggerImplementation
import org.ktorm.schema.*
import java.math.BigDecimal
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.time.*
import java.util.*
import kotlin.reflect.*
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField

private val logger: Logger = detectLoggerImplementation()

internal val String.sqlName: String
    get() {
        val sb = StringBuilder()
        val iterator = iterator()
        sb.append(iterator.nextChar().toLowerCase())
        iterator.forEach {
            if (it.isUpperCase()) {
                sb.append('_')
                sb.append(it.toLowerCase())
            } else {
                sb.append(it)
            }
        }
        return sb.toString()
    }
internal val KClass<*>.tableName: String
    get() = findAnnotation<TableName>()?.name ?: simpleName!!.sqlName
internal val Class<*>.tableName: String
    get() = getAnnotation(TableName::class.java)?.name ?: simpleName.sqlName
internal val KProperty<*>.tableFieldName: String
    get() = javaField?.getAnnotation(TableField::class.java)?.name ?: name.sqlName

/**
 * get AutoTable instance of property.
 */
public val KProperty<*>.table: AutoTable<Any>
    @Suppress("UNCHECKED_CAST")
    get() = AutoTable[javaField!!.declaringClass] as AutoTable<Any>

/**
 * get Column instance of property.
 */
public val <T : Any> KProperty<T?>.column: Column<T>
    get() = table[this]

/**
 * get AutoTable instance of property.
 */
public inline val <reified T : Any> KProperty1<out T, *>.table: AutoTable<T>
    get() = AutoTable[T::class.java]

/**
 * get one data instance of query.
 */
public fun <T> Query.getOne(
    transform: (rowSet: QueryRowSet) -> T,
): T? = if (rowSet.next()) {
    transform(rowSet)
} else {
    null
}

/**
 * get one data instance of query.
 */
public inline fun <reified T : Any> Query.getOne(): T? = if (rowSet.next()) {
    AutoTable[T::class].createEntity(rowSet)
} else {
    null
}

/**
 * convert data contains in Query to List.
 */
public inline fun <reified T : Any> Query.toList(
    table: AutoTable<T>,
): List<T> = map {
    table.createEntity(it)
}

/**
 * Define a column typed of [BooleanSqlType].
 */
public fun <E : Any> BaseTable<E>.boolean(
    field: KProperty1<E, Boolean?>,
): Column<Boolean> = boolean(field.tableFieldName)

/**
 * Define a column typed of [IntSqlType].
 */
public fun <E : Any> BaseTable<E>.int(
    field: KProperty1<E, Int?>,
): Column<Int> = int(field.tableFieldName)

/**
 * Define a column typed of [LongSqlType].
 */
public fun <E : Any> BaseTable<E>.long(
    field: KProperty1<E, Long?>,
): Column<Long> = long(field.tableFieldName)

/**
 * Define a column typed of [FloatSqlType].
 */
public fun <E : Any> BaseTable<E>.float(
    field: KProperty1<E, Float?>,
): Column<Float> = float(field.tableFieldName)

/**
 * Define a column typed of [DoubleSqlType].
 */
public fun <E : Any> BaseTable<E>.double(
    field: KProperty1<E, Double?>,
): Column<Double> = double(field.tableFieldName)

/**
 * Define a column typed of [DecimalSqlType].
 */
public fun <E : Any> BaseTable<E>.decimal(
    field: KProperty1<E, BigDecimal?>,
): Column<BigDecimal> = decimal(field.tableFieldName)

/**
 * Define a column typed of [VarcharSqlType].
 */
public fun <E : Any> BaseTable<E>.varchar(
    field: KProperty1<E, String?>,
): Column<String> = varchar(field.tableFieldName)

/**
 * Define a column typed of [TextSqlType].
 */
public fun <E : Any> BaseTable<E>.text(
    field: KProperty1<E, String?>,
): Column<String> = text(field.tableFieldName)

/**
 * Define a column typed of [BlobSqlType].
 */
public fun <E : Any> BaseTable<E>.blob(
    field: KProperty1<E, ByteArray?>,
): Column<ByteArray> = blob(field.tableFieldName)

/**
 * Define a column typed of [BytesSqlType].
 */
public fun <E : Any> BaseTable<E>.bytes(
    field: KProperty1<E, ByteArray?>,
): Column<ByteArray> = bytes(field.tableFieldName)

/**
 * Define a column typed of [TimestampSqlType].
 */
public fun <E : Any> BaseTable<E>.jdbcTimestamp(
    field: KProperty1<E, Timestamp?>,
): Column<Timestamp> = jdbcTimestamp(field.tableFieldName)

/**
 * Define a column typed of [DateSqlType].
 */
public fun <E : Any> BaseTable<E>.jdbcDate(
    field: KProperty1<E, Date?>,
): Column<Date> = jdbcDate(field.tableFieldName)

/**
 * Define a column typed of [TimeSqlType].
 */
public fun <E : Any> BaseTable<E>.jdbcTime(
    field: KProperty1<E, Time?>,
): Column<Time> = jdbcTime(field.tableFieldName)

/**
 * Define a column typed of [InstantSqlType].
 */
public fun <E : Any> BaseTable<E>.timestamp(
    field: KProperty1<E, Instant?>,
): Column<Instant> = timestamp(field.tableFieldName)

/**
 * Define a column typed of [LocalDateTimeSqlType].
 */
public fun <E : Any> BaseTable<E>.datetime(
    field: KProperty1<E, LocalDateTime?>,
): Column<LocalDateTime> = datetime(field.tableFieldName)

/**
 * Define a column typed of [LocalTimeSqlType].
 */
public fun <E : Any> BaseTable<E>.date(
    field: KProperty1<E, LocalDate?>,
): Column<LocalDate> = date(field.tableFieldName)

/**
 * Define a column typed of [LocalTimeSqlType].
 */
public fun <E : Any> BaseTable<E>.time(
    field: KProperty1<E, LocalTime?>,
): Column<LocalTime> = time(field.tableFieldName)

/**
 * Define a column typed of [MonthDaySqlType], instances of [MonthDay] are saved as strings in format `MM-dd`.
 */
public fun <E : Any> BaseTable<E>.monthDay(
    field: KProperty1<E, MonthDay?>,
): Column<MonthDay> = monthDay(field.tableFieldName)

/**
 * Define a column typed of [YearMonthSqlType], instances of [YearMonth] are saved as strings in format `yyyy-MM`.
 */
public fun <E : Any> BaseTable<E>.yearMonth(
    field: KProperty1<E, YearMonth?>,
): Column<YearMonth> = yearMonth(field.tableFieldName)

/**
 * Define a column typed of [YearSqlType], instances of [Year] are saved as integers.
 */
public fun <E : Any> BaseTable<E>.year(
    field: KProperty1<E, Year?>,
): Column<Year> = year(field.tableFieldName)

/**
 * Define a column typed of [UuidSqlType].
 */
public fun <E : Any> BaseTable<E>.uuid(
    field: KProperty1<E, UUID?>,
): Column<UUID> = uuid(field.tableFieldName)

/**
 * Define a column typed of [C].
 */
public fun <E : Any, C : Enum<C>> BaseTable<E>.enum(
    field: KProperty1<E, C?>,
    type: Class<C>,
): Column<C> = registerColumn(field.tableFieldName, EnumSqlType(type))

/**
 * inject value to an property of an instance.
 */
internal fun <T : Any> Any.inject(target: T, property: KProperty1<T, Any?>) {
    when (property) {
        is KMutableProperty1<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            property as KMutableProperty1<T, Any?>
            property.isAccessible = true
            try {
                @OptIn(ExperimentalStdlibApi::class)
                property.set(
                    target,
                    cast(
                        this,
                        property.returnType.javaType as Class<*>
                    ) ?: return
                )
                // } catch (e: ClassCastException) {
            } catch (e: Exception) {
                logger.trace("inject ${property.name} failed", e)
            }
        }
        else -> {
            val field = property.javaField ?: return
            field.isAccessible = true
            try {
                field.set(target, cast(this, field.type) ?: return)
                // } catch (e: ClassCastException) {
            } catch (e: Exception) {
                logger.trace("inject ${property.name} failed", e)
            }
        }
    }
}

internal fun cast(
    source: Any,
    target: Class<*>,
): Any? = when {
    target.isInstance(source) -> source
    else -> when (target) {
        Byte::class.java -> if (source is Number) source.toByte() else source.toString().toByteOrNull()
        Char::class.java -> if (source is Number) source.toChar() else source.toString().toIntOrNull()?.toChar()
        Short::class.java -> if (source is Number) source.toShort() else source.toString().toShortOrNull()
        Int::class.java -> if (source is Number) source.toInt() else source.toString().toIntOrNull()
        Long::class.java -> if (source is Number) source.toLong() else source.toString().toLongOrNull()
        Float::class.java -> if (source is Number) source.toFloat() else source.toString().toFloatOrNull()
        Double::class.java -> if (source is Number) source.toDouble() else source.toString().toDoubleOrNull()
        Boolean::class.java -> if (source is Number) source != 0 else source.toString().toBoolean()
        java.lang.Byte::class.java -> if (source is Number) source.toByte() else source.toString().toByteOrNull()
        java.lang.Character::class.java -> if (source is Number) {
            source.toChar()
        } else {
            source.toString().toIntOrNull()?.toChar()
        }
        java.lang.Short::class.java -> if (source is Number) source.toShort() else source.toString().toShortOrNull()
        java.lang.Integer::class.java -> if (source is Number) source.toInt() else source.toString().toIntOrNull()
        java.lang.Long::class.java -> if (source is Number) source.toLong() else source.toString().toLongOrNull()
        java.lang.Float::class.java -> if (source is Number) source.toFloat() else source.toString().toFloatOrNull()
        java.lang.Double::class.java -> if (source is Number) source.toDouble() else source.toString().toDoubleOrNull()
        java.lang.Boolean::class.java -> if (source is Number) source != 0 else source.toString().toBoolean()
        String::class.java -> source.toString()
        else -> source
    }
}
