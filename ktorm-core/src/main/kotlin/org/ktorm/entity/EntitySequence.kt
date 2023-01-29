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

package org.ktorm.entity

import org.ktorm.database.Database
import org.ktorm.database.DialectFeatureNotSupportedException
import org.ktorm.dsl.*
import org.ktorm.expression.OrderByExpression
import org.ktorm.expression.SelectExpression
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import org.ktorm.schema.ColumnDeclaring
import java.sql.ResultSet
import java.util.*
import kotlin.NoSuchElementException
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.collections.LinkedHashMap
import kotlin.collections.LinkedHashSet
import kotlin.experimental.ExperimentalTypeInference
import kotlin.math.min

/**
 * Represents a sequence of entity objects. As the name implies, the style and use pattern of Ktorm's entity sequence
 * APIs are highly similar to [kotlin.sequences.Sequence] and the extension functions in Kotlin standard lib, as it
 * provides many extension functions with the same names, such as [filter], [map], [reduce], etc.
 *
 * To create an [EntitySequence], we can use the extension function [sequenceOf]:
 *
 * ```kotlin
 * val sequence = database.sequenceOf(Employees)
 * ```
 *
 * Now we got a default sequence, which can obtain all employees from the table. Please know that Ktorm doesn't execute
 * the query right now. The sequence provides an iterator of type `Iterator<Employee>`, only when we iterate the
 * sequence using the iterator, the query is executed. The following code prints all employees using a for-each loop:
 *
 * ```kotlin
 * for (employee in sequence) {
 *     println(employee)
 * }
 * ```
 *
 * This class wraps a [Query] object, and it’s iterator exactly wraps the query’s iterator. When an entity sequence is
 * iterated, its internal query is executed, and the [entityExtractor] function is applied to create an entity object
 * for each row.
 *
 * Most of the entity sequence APIs are provided as extension functions, which can be divided into two groups:
 *
 * - **Intermediate operations:** these functions don’t execute the internal queries but return new-created sequence
 * objects applying some modifications. For example, the [filter] function creates a new sequence object with the filter
 * condition given by its parameter. The return types of intermediate operations are usually [EntitySequence], so we
 * can call other sequence functions continuously in chaining style.
 *
 * - **Terminal operations:** the return types of these functions are usually a collection or a computed result, as
 * they execute the queries right now, obtain their results and perform some calculations on them. E.g. [toList],
 * [reduce], etc.
 *
 * For the list of sequence operations available, see the extension functions below.
 */
public class EntitySequence<E : Any, T : BaseTable<E>>(

    /**
     * The [Database] instance that the internal query is running on.
     */
    public val database: Database,

    /**
     * The source table from which elements are obtained.
     */
    public val sourceTable: T,

    /**
     * The SQL expression to be executed by this sequence when obtaining elements.
     */
    public val expression: SelectExpression,

    /**
     * The function used to extract entity objects for each result row.
     */
    public val entityExtractor: (row: QueryRowSet) -> E
) {
    /**
     * The internal query of this sequence to be executed, created by [database] and [expression].
     */
    public val query: Query = Query(database, expression)

    /**
     * The executable SQL string of the internal query.
     *
     * This property is delegated to [Query.sql], more details can be found in its documentation.
     */
    public val sql: String get() = query.sql

    /**
     * The [ResultSet] object of the internal query, lazy initialized after first access, obtained from the database by
     * executing the generated SQL.
     *
     * This property is delegated to [Query.rowSet], more details can be found in its documentation.
     */
    public val rowSet: QueryRowSet get() = query.rowSet

    /**
     * The total records count of this query ignoring the pagination params.
     */
    @Deprecated("The property is deprecated, use totalRecordsInAllPages instead", ReplaceWith("totalRecordsInAllPages"))
    public val totalRecords: Int get() = totalRecordsInAllPages

    /**
     * The total records count of this query ignoring the pagination params.
     *
     * This property is delegated to [Query.totalRecordsInAllPages], more details can be found in its documentation.
     */
    public val totalRecordsInAllPages: Int get() = query.totalRecordsInAllPages

    /**
     * Return a copy of this [EntitySequence] with the [expression] modified.
     */
    public fun withExpression(expression: SelectExpression): EntitySequence<E, T> {
        return EntitySequence(database, sourceTable, expression, entityExtractor)
    }

    /**
     * Create a [kotlin.sequences.Sequence] instance that wraps this original entity sequence returning all the
     * elements when being iterated.
     */
    public fun asKotlinSequence(): Sequence<E> {
        return Sequence { iterator() }
    }

    /**
     * Return an iterator over the elements of this sequence.
     */
    @Suppress("IteratorNotThrowingNoSuchElementException")
    public operator fun iterator(): Iterator<E> = object : Iterator<E> {
        private val queryIterator = query.iterator()

        override fun hasNext(): Boolean {
            return queryIterator.hasNext()
        }

        override fun next(): E {
            return entityExtractor(queryIterator.next())
        }
    }
}

/**
 * Create an [EntitySequence] from the specific table.
 */
public fun <E : Any, T : BaseTable<E>> Database.sequenceOf(
    table: T, withReferences: Boolean = true
): EntitySequence<E, T> {
    val query = if (withReferences) from(table).joinReferencesAndSelect() else from(table).select(table.columns)
    val entityExtractor = { row: QueryRowSet -> table.createEntity(row, withReferences) }
    return EntitySequence(this, table, query.expression as SelectExpression, entityExtractor)
}

/**
 * Create an [EntitySequence] from the specific table.
 */
@JvmName("sequenceOfNothing")
@Deprecated("Entity sequence not supported because the table doesn't bind to an entity class, use SQL DSL instead. ")
public fun <T : BaseTable<Nothing>> Database.sequenceOf(
    table: T, withReferences: Boolean = true
): EntitySequence<Nothing, T> {
    val query = if (withReferences) from(table).joinReferencesAndSelect() else from(table).select(table.columns)
    val entityExtractor = { row: QueryRowSet -> table.createEntity(row, withReferences) }
    return EntitySequence(this, table, query.expression as SelectExpression, entityExtractor)
}

/**
 * Append all elements to the given [destination] collection.
 *
 * The operation is terminal.
 */
public fun <E : Any, C : MutableCollection<in E>> EntitySequence<E, *>.toCollection(destination: C): C {
    for (element in this) destination += element
    return destination
}

/**
 * Return a [List] containing all the elements of this sequence.
 *
 * The operation is terminal.
 */
public fun <E : Any> EntitySequence<E, *>.toList(): List<E> {
    return toCollection(ArrayList())
}

/**
 * Return a [MutableList] containing all the elements of this sequence.
 *
 * The operation is terminal.
 */
public fun <E : Any> EntitySequence<E, *>.toMutableList(): MutableList<E> {
    return toCollection(ArrayList())
}

/**
 * Return a [Set] containing all the elements of this sequence.
 *
 * The returned set preserves the element iteration order of the original sequence.
 *
 * The operation is terminal.
 */
public fun <E : Any> EntitySequence<E, *>.toSet(): Set<E> {
    return toCollection(LinkedHashSet())
}

/**
 * Return a [MutableSet] containing all the elements of this sequence.
 *
 * The returned set preserves the element iteration order of the original sequence.
 *
 * The operation is terminal.
 */
public fun <E : Any> EntitySequence<E, *>.toMutableSet(): MutableSet<E> {
    return toCollection(LinkedHashSet())
}

/**
 * Return a [HashSet] containing all the elements of this sequence.
 *
 * The operation is terminal.
 */
public fun <E : Any> EntitySequence<E, *>.toHashSet(): HashSet<E> {
    return toCollection(HashSet())
}

/**
 * Return a [SortedSet] containing all the elements of this sequence.
 *
 * The operation is terminal.
 */
public fun <E> EntitySequence<E, *>.toSortedSet(): SortedSet<E> where E : Any, E : Comparable<E> {
    return toCollection(TreeSet())
}

/**
 * Return a [SortedSet] containing all the elements of this sequence.
 *
 * Elements in the set returned are sorted according to the given [comparator].
 *
 * The operation is terminal.
 */
public fun <E> EntitySequence<E, *>.toSortedSet(
    comparator: Comparator<in E>
): SortedSet<E> where E : Any, E : Comparable<E> {
    return toCollection(TreeSet(comparator))
}

/**
 * Return a sequence customizing the selected columns of the internal query.
 *
 * The operation is intermediate.
 */
public inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.filterColumns(
    selector: (T) -> List<Column<*>>
): EntitySequence<E, T> {
    val columns = selector(sourceTable)
    if (columns.isEmpty()) {
        return this
    } else {
        return this.withExpression(expression.copy(columns = columns.map { it.aliased(it.label) }))
    }
}

/**
 * Return a sequence containing only elements matching the given [predicate].
 *
 * The operation is intermediate.
 */
public inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.filter(
    predicate: (T) -> ColumnDeclaring<Boolean>
): EntitySequence<E, T> {
    if (expression.where == null) {
        return this.withExpression(expression.copy(where = predicate(sourceTable).asExpression()))
    } else {
        return this.withExpression(expression.copy(where = expression.where and predicate(sourceTable)))
    }
}

/**
 * Return a sequence containing only elements not matching the given [predicate].
 *
 * The operation is intermediate.
 */
public inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.filterNot(
    predicate: (T) -> ColumnDeclaring<Boolean>
): EntitySequence<E, T> {
    return filter { !predicate(it) }
}

/**
 * Append all elements matching the given [predicate] to the given [destination].
 *
 * The operation is terminal.
 */
public inline fun <E : Any, T : BaseTable<E>, C : MutableCollection<in E>> EntitySequence<E, T>.filterTo(
    destination: C,
    predicate: (T) -> ColumnDeclaring<Boolean>
): C {
    return filter(predicate).toCollection(destination)
}

/**
 * Append all elements not matching the given [predicate] to the given [destination].
 *
 * The operation is terminal.
 */
public inline fun <E : Any, T : BaseTable<E>, C : MutableCollection<in E>> EntitySequence<E, T>.filterNotTo(
    destination: C,
    predicate: (T) -> ColumnDeclaring<Boolean>
): C {
    return filterNot(predicate).toCollection(destination)
}

/**
 * Return a [List] containing the results of applying the given [transform] function
 * to each element in the original sequence.
 *
 * The operation is terminal.
 */
public inline fun <E : Any, R> EntitySequence<E, *>.map(transform: (E) -> R): List<R> {
    return mapTo(ArrayList(), transform)
}

/**
 * Apply the given [transform] function to each element of the original sequence
 * and append the results to the given [destination].
 *
 * The operation is terminal.
 */
public inline fun <E : Any, R, C : MutableCollection<in R>> EntitySequence<E, *>.mapTo(
    destination: C,
    transform: (E) -> R
): C {
    for (element in this) destination += transform(element)
    return destination
}

/**
 * Return a [List] containing only the non-null results of applying the given [transform] function
 * to each element in the original sequence.
 *
 * The operation is terminal.
 *
 * @since 3.0.0
 */
public inline fun <E : Any, R : Any> EntitySequence<E, *>.mapNotNull(transform: (E) -> R?): List<R> {
    return mapNotNullTo(ArrayList(), transform)
}

/**
 * Apply the given [transform] function to each element in the original sequence
 * and append only the non-null results to the given [destination].
 *
 * The operation is terminal.
 *
 * @since 3.0.0
 */
public inline fun <E : Any, R : Any, C : MutableCollection<in R>> EntitySequence<E, *>.mapNotNullTo(
    destination: C,
    transform: (E) -> R?
): C {
    forEach { element -> transform(element)?.let { destination += it } }
    return destination
}

/**
 * Return a [List] containing the results of applying the given [transform] function
 * to each element and its index in the original sequence.
 *
 * The [transform] function takes the index of an element and the element itself and
 * returns the result of the transform applied to the element.
 *
 * The operation is terminal.
 */
public inline fun <E : Any, R> EntitySequence<E, *>.mapIndexed(transform: (index: Int, E) -> R): List<R> {
    return mapIndexedTo(ArrayList(), transform)
}

/**
 * Apply the given [transform] function to each element and its index in the original sequence
 * and append the results to the given [destination].
 *
 * The [transform] function takes the index of an element and the element itself and
 * returns the result of the transform applied to the element.
 *
 * The operation is terminal.
 */
public inline fun <E : Any, R, C : MutableCollection<in R>> EntitySequence<E, *>.mapIndexedTo(
    destination: C,
    transform: (index: Int, E) -> R
): C {
    var index = 0
    return mapTo(destination) { transform(index++, it) }
}

/**
 * Return a [List] containing only the non-null results of applying the given [transform] function
 * to each element and its index in the original sequence.
 *
 * The [transform] function takes the index of an element and the element itself and
 * returns the result of the transform applied to the element.
 *
 * The operation is terminal.
 *
 * @since 3.0.0
 */
public inline fun <E : Any, R : Any> EntitySequence<E, *>.mapIndexedNotNull(transform: (index: Int, E) -> R?): List<R> {
    return mapIndexedNotNullTo(ArrayList(), transform)
}

/**
 * Apply the given [transform] function to each element and its index in the original sequence
 * and append only the non-null results to the given [destination].
 *
 * The [transform] function takes the index of an element and the element itself and
 * returns the result of the transform applied to the element.
 *
 * The operation is terminal.
 *
 * @since 3.0.0
 */
public inline fun <E : Any, R : Any, C : MutableCollection<in R>> EntitySequence<E, *>.mapIndexedNotNullTo(
    destination: C,
    transform: (index: Int, E) -> R?
): C {
    forEachIndexed { index, element -> transform(index, element)?.let { destination += it } }
    return destination
}

/**
 * Return a single list of all elements yielded from results of [transform] function being invoked
 * on each element of original sequence.
 *
 * The operation is terminal.
 *
 * @since 3.0.0
 */
public inline fun <E : Any, R> EntitySequence<E, *>.flatMap(transform: (E) -> Iterable<R>): List<R> {
    return flatMapTo(ArrayList(), transform)
}

/**
 * Append all elements yielded from results of [transform] function being invoked on each element
 * of original sequence, to the given [destination].
 *
 * The operation is terminal.
 *
 * @since 3.0.0
 */
public inline fun <E : Any, R, C : MutableCollection<in R>> EntitySequence<E, *>.flatMapTo(
    destination: C,
    transform: (E) -> Iterable<R>
): C {
    for (element in this) destination += transform(element)
    return destination
}

/**
 * Return a single list of all elements yielded from results of [transform] function being invoked
 * on each element and its index in the original sequence.
 *
 * The operation is terminal.
 *
 * @since 3.1.0
 */
public inline fun <E : Any, R> EntitySequence<E, *>.flatMapIndexed(transform: (index: Int, E) -> Iterable<R>): List<R> {
    return flatMapIndexedTo(ArrayList(), transform)
}

/**
 * Append all elements yielded from results of [transform] function being invoked on each element
 * and its index in the original sequence, to the given [destination].
 *
 * The operation is terminal.
 *
 * @since 3.1.0
 */
public inline fun <E : Any, R, C : MutableCollection<in R>> EntitySequence<E, *>.flatMapIndexedTo(
    destination: C,
    transform: (index: Int, E) -> Iterable<R>
): C {
    var index = 0
    return flatMapTo(destination) { transform(index++, it) }
}

/**
 * Customize the selected columns of the internal query by the given [columnSelector] function, and return a [List]
 * containing the query results.
 *
 * This function is similar to [EntitySequence.map], but the [columnSelector] closure accepts the current table
 * object [T] as the parameter, so what we get in the closure by `it` is the table object instead of an entity
 * element. Besides, the function’s return type is `ColumnDeclaring<C>`, and we should return a column or expression
 * to customize the `select` clause of the generated SQL.
 *
 * Ktorm also supports selecting two or more columns, we just need to wrap our selected columns by [tupleOf]
 * in the closure, then the function’s return type becomes `List<TupleN<C1?, C2?, .. Cn?>>`.
 *
 * The operation is terminal.
 *
 * @param isDistinct specify if the query is distinct, the generated SQL becomes `select distinct` if it's set to true.
 * @param columnSelector a function in which we should return a column or expression to be selected.
 * @return a list of the query results.
 */
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
public inline fun <E : Any, T : BaseTable<E>, C : Any> EntitySequence<E, T>.mapColumns(
    isDistinct: Boolean = false,
    columnSelector: (T) -> ColumnDeclaring<C>
): List<C?> {
    return mapColumnsTo(ArrayList(), isDistinct, columnSelector)
}

/**
 * Customize the selected columns of the internal query by the given [columnSelector] function, and append the query
 * results to the given [destination].
 *
 * This function is similar to [EntitySequence.mapTo], but the [columnSelector] closure accepts the current table
 * object [T] as the parameter, so what we get in the closure by `it` is the table object instead of an entity
 * element. Besides, the function’s return type is `ColumnDeclaring<C>`, and we should return a column or expression
 * to customize the `select` clause of the generated SQL.
 *
 * Ktorm also supports selecting two or more columns, we just need to wrap our selected columns by [tupleOf]
 * in the closure, then the function’s return type becomes `List<TupleN<C1?, C2?, .. Cn?>>`.
 *
 * The operation is terminal.
 *
 * @param destination a [MutableCollection] used to store the results.
 * @param isDistinct specify if the query is distinct, the generated SQL becomes `select distinct` if it's set to true.
 * @param columnSelector a function in which we should return a column or expression to be selected.
 * @return the [destination] collection of the query results.
 */
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
public inline fun <E : Any, T : BaseTable<E>, C, R> EntitySequence<E, T>.mapColumnsTo(
    destination: R,
    isDistinct: Boolean = false,
    columnSelector: (T) -> ColumnDeclaring<C>
): R where C : Any, R : MutableCollection<in C?> {
    val column = columnSelector(sourceTable)

    val expr = expression.copy(
        columns = listOf(column.aliased(null)),
        isDistinct = isDistinct
    )

    return Query(database, expr).mapTo(destination) { row -> column.sqlType.getResult(row, 1) }
}

/**
 * Customize the selected columns of the internal query by the given [columnSelector] function, and return a [List]
 * containing the non-null results.
 *
 * This function is similar to [EntitySequence.mapColumns], but null results are filtered, more details can be found
 * in its documentation.
 *
 * The operation is terminal.
 *
 * @param isDistinct specify if the query is distinct, the generated SQL becomes `select distinct` if it's set to true.
 * @param columnSelector a function in which we should return a column or expression to be selected.
 */
public inline fun <E : Any, T : BaseTable<E>, C : Any> EntitySequence<E, T>.mapColumnsNotNull(
    isDistinct: Boolean = false,
    columnSelector: (T) -> ColumnDeclaring<C>
): List<C> {
    return mapColumnsNotNullTo(ArrayList(), isDistinct, columnSelector)
}

/**
 * Customize the selected columns of the internal query by the given [columnSelector] function, and append non-null
 * results to the given [destination].
 *
 * This function is similar to [EntitySequence.mapColumnsTo], but null results are filtered, more details can be found
 * in its documentation.
 *
 * The operation is terminal.
 *
 * @param destination a [MutableCollection] used to store the results.
 * @param isDistinct specify if the query is distinct, the generated SQL becomes `select distinct` if it's set to true.
 * @param columnSelector a function in which we should return a column or expression to be selected.
 */
public inline fun <E : Any, T : BaseTable<E>, C, R> EntitySequence<E, T>.mapColumnsNotNullTo(
    destination: R,
    isDistinct: Boolean = false,
    columnSelector: (T) -> ColumnDeclaring<C>
): R where C : Any, R : MutableCollection<in C> {
    val column = columnSelector(sourceTable)

    val expr = expression.copy(
        columns = listOf(column.aliased(null)),
        isDistinct = isDistinct
    )

    return Query(database, expr).mapNotNullTo(destination) { row -> column.sqlType.getResult(row, 1) }
}

/**
 * Return a sequence sorting elements by multiple columns, in ascending or descending order. For example,
 * `sortedBy({ it.col1.asc() }, { it.col2.desc() })`.
 *
 * The operation is intermediate.
 */
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
public fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.sortedBy(
    vararg selectors: (T) -> OrderByExpression
): EntitySequence<E, T> {
    return this.withExpression(expression.copy(orderBy = selectors.map { it(sourceTable) }))
}

/**
 * Return a sequence sorting elements by a column, in ascending or descending order. For example,
 * `sortedBy { it.col.asc() }`
 *
 * The operation is intermediate.
 */
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
public inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.sortedBy(
    selector: (T) -> OrderByExpression
): EntitySequence<E, T> {
    return this.withExpression(expression.copy(orderBy = listOf(selector(sourceTable))))
}

/**
 * Return a sequence sorting elements by the specific column in ascending order.
 *
 * The operation is intermediate.
 */
@JvmName("sortedByAscending")
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
public inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.sortedBy(
    selector: (T) -> ColumnDeclaring<*>
): EntitySequence<E, T> {
    return this.withExpression(expression.copy(orderBy = listOf(selector(sourceTable).asc())))
}

/**
 * Return a sequence sorting elements by the specific column in descending order.
 *
 * The operation is intermediate.
 */
public inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.sortedByDescending(
    selector: (T) -> ColumnDeclaring<*>
): EntitySequence<E, T> {
    return this.withExpression(expression.copy(orderBy = listOf(selector(sourceTable).desc())))
}

/**
 * Returns a sequence containing all elements except first [n] elements.
 *
 * Note that this function is implemented based on the pagination feature of the specific databases. It's known that
 * there is a uniform standard for SQL language, but the SQL standard doesn't say how to implement paging queries,
 * different databases provide different implementations on that. So we have to enable a dialect if we need to use this
 * function, otherwise an exception will be thrown.
 *
 * The operation is intermediate.
 */
public fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.drop(n: Int): EntitySequence<E, T> {
    if (n == 0) {
        return this
    } else {
        val offset = expression.offset ?: 0
        return this.withExpression(expression.copy(offset = offset + n))
    }
}

/**
 * Returns a sequence containing first [n] elements.
 *
 * Note that this function is implemented based on the pagination feature of the specific databases. It's known that
 * there is a uniform standard for SQL language, but the SQL standard doesn't say how to implement paging queries,
 * different databases provide different implementations on that. So we have to enable a dialect if we need to use this
 * function, otherwise an exception will be thrown.
 *
 * The operation is intermediate.
 */
public fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.take(n: Int): EntitySequence<E, T> {
    val limit = expression.limit ?: Int.MAX_VALUE
    return this.withExpression(expression.copy(limit = min(limit, n)))
}

/**
 * Perform an aggregation given by [aggregationSelector] for all elements in the sequence,
 * and return the aggregate result.
 *
 * Ktorm also supports aggregating two or more columns, we just need to wrap our aggregate expressions by
 * [tupleOf] in the closure, then the function’s return type becomes `TupleN<C1?, C2?, .. Cn?>`.
 *
 * The operation is terminal.
 *
 * @param aggregationSelector a function that accepts the source table and returns the aggregate expression.
 * @return the aggregate result.
 */
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
public inline fun <E : Any, T : BaseTable<E>, C : Any> EntitySequence<E, T>.aggregateColumns(
    aggregationSelector: (T) -> ColumnDeclaring<C>
): C? {
    val aggregation = aggregationSelector(sourceTable)

    val expr = expression.copy(
        columns = listOf(aggregation.aliased(null))
    )

    val rowSet = Query(database, expr).rowSet

    if (rowSet.size() == 1) {
        check(rowSet.next())
        return aggregation.sqlType.getResult(rowSet, 1)
    } else {
        val (sql, _) = database.formatExpression(expr, beautifySql = true)
        throw IllegalStateException("Expected 1 row but ${rowSet.size()} returned from sql: \n\n$sql")
    }
}

/**
 * Return the number of elements in this sequence.
 *
 * The operation is terminal.
 */
public fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.count(): Int {
    val count = aggregateColumns { org.ktorm.dsl.count() }
    return count ?: error("Count expression returns null, which should never happens.")
}

/**
 * Return the number of elements matching the given [predicate].
 *
 * The operation is terminal.
 */
public inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.count(
    predicate: (T) -> ColumnDeclaring<Boolean>
): Int {
    return filter(predicate).count()
}

/**
 * Return `true` if the sequence has no elements.
 *
 * The operation is terminal.
 *
 * @since 2.7
 */
public fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.isEmpty(): Boolean {
    return count() == 0
}

/**
 * Return `true` if the sequence has at lease one element.
 *
 * The operation is terminal.
 *
 * @since 2.7
 */
public fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.isNotEmpty(): Boolean {
    return count() > 0
}

/**
 * Return `true` if the sequence has no elements.
 *
 * The operation is terminal.
 */
public fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.none(): Boolean {
    return count() == 0
}

/**
 * Return `true` if no elements match the given [predicate].
 *
 * The operation is terminal.
 */
public inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.none(
    predicate: (T) -> ColumnDeclaring<Boolean>
): Boolean {
    return count(predicate) == 0
}

/**
 * Return `true` if the sequence has at lease one element.
 *
 * The operation is terminal.
 */
public fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.any(): Boolean {
    return count() > 0
}

/**
 * Return `true` if at least one element matches the given [predicate].
 *
 * The operation is terminal.
 */
public inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.any(
    predicate: (T) -> ColumnDeclaring<Boolean>
): Boolean {
    return count(predicate) > 0
}

/**
 * Return `true` if all elements match the given [predicate].
 *
 * The operation is terminal.
 */
public inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.all(
    predicate: (T) -> ColumnDeclaring<Boolean>
): Boolean {
    return none { !predicate(it) }
}

/**
 * Return the sum of the column given by [selector] in this sequence.
 *
 * The operation is terminal.
 */
public inline fun <E : Any, T : BaseTable<E>, C : Number> EntitySequence<E, T>.sumBy(
    selector: (T) -> ColumnDeclaring<C>
): C? {
    return aggregateColumns { sum(selector(it)) }
}

/**
 * Return the max value of the column given by [selector] in this sequence.
 *
 * The operation is terminal.
 */
public inline fun <E : Any, T : BaseTable<E>, C : Comparable<C>> EntitySequence<E, T>.maxBy(
    selector: (T) -> ColumnDeclaring<C>
): C? {
    return aggregateColumns { max(selector(it)) }
}

/**
 * Return the min value of the column given by [selector] in this sequence.
 *
 * The operation is terminal.
 */
public inline fun <E : Any, T : BaseTable<E>, C : Comparable<C>> EntitySequence<E, T>.minBy(
    selector: (T) -> ColumnDeclaring<C>
): C? {
    return aggregateColumns { min(selector(it)) }
}

/**
 * Return the average value of the column given by [selector] in this sequence.
 *
 * The operation is terminal.
 */
public inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.averageBy(
    selector: (T) -> ColumnDeclaring<out Number>
): Double? {
    return aggregateColumns { avg(selector(it)) }
}

/**
 * Return a [Map] containing key-value pairs provided by [transform] function applied to elements of the given sequence.
 *
 * If any of two pairs would have the same key the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original sequence.
 *
 * The operation is terminal.
 */
public inline fun <E : Any, K, V> EntitySequence<E, *>.associate(transform: (E) -> Pair<K, V>): Map<K, V> {
    return associateTo(LinkedHashMap(), transform)
}

/**
 * Return a [Map] containing the elements from the given sequence indexed by the key returned from [keySelector]
 * function applied to each element.
 *
 * If any two elements have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original sequence.
 *
 * The operation is terminal.
 */
public inline fun <E : Any, K> EntitySequence<E, *>.associateBy(keySelector: (E) -> K): Map<K, E> {
    return associateByTo(LinkedHashMap(), keySelector)
}

/**
 * Return a [Map] containing the values provided by [valueTransform] and indexed by [keySelector] functions
 * applied to elements of the given sequence.
 *
 * If any two elements have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original sequence.
 *
 * The operation is terminal.
 */
public inline fun <E : Any, K, V> EntitySequence<E, *>.associateBy(
    keySelector: (E) -> K,
    valueTransform: (E) -> V
): Map<K, V> {
    return associateByTo(LinkedHashMap(), keySelector, valueTransform)
}

/**
 * Return a [Map] where keys are elements from the given sequence and values are produced by the [valueSelector]
 * function applied to each element.
 *
 * If any two elements are equal, the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original sequence.
 *
 * The operation is terminal.
 */
public inline fun <K : Entity<K>, V> EntitySequence<K, *>.associateWith(valueSelector: (K) -> V): Map<K, V> {
    return associateWithTo(LinkedHashMap(), valueSelector)
}

/**
 * Populate and return the [destination] mutable map with key-value pairs provided by [transform] function applied
 * to each element of the given sequence.
 *
 * If any of two pairs would have the same key the last one gets added to the map.
 *
 * The operation is terminal.
 */
public inline fun <E : Any, K, V, M : MutableMap<in K, in V>> EntitySequence<E, *>.associateTo(
    destination: M,
    transform: (E) -> Pair<K, V>
): M {
    for (element in this) destination += transform(element)
    return destination
}

/**
 * Populate and return the [destination] mutable map with key-value pairs, where key is provided by the [keySelector]
 * function applied to each element of the given sequence and value is the element itself.
 *
 * If any two elements have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The operation is terminal.
 */
public inline fun <E : Any, K, M : MutableMap<in K, in E>> EntitySequence<E, *>.associateByTo(
    destination: M,
    keySelector: (E) -> K
): M {
    for (element in this) destination.put(keySelector(element), element)
    return destination
}

/**
 * Populate and return the [destination] mutable map with key-value pairs, where key is provided by the [keySelector]
 * function and value is provided by the [valueTransform] function applied to elements of the given sequence.
 *
 * If any two elements have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The operation is terminal.
 */
public inline fun <E : Any, K, V, M : MutableMap<in K, in V>> EntitySequence<E, *>.associateByTo(
    destination: M,
    keySelector: (E) -> K,
    valueTransform: (E) -> V
): M {
    for (element in this) destination.put(keySelector(element), valueTransform(element))
    return destination
}

/**
 * Populate and return the [destination] mutable map with key-value pairs for each element of the given sequence,
 * where key is the element itself and value is provided by the [valueSelector] function applied to that key.
 *
 * If any two elements are equal, the last one overwrites the former value in the map.
 *
 * The operation is terminal.
 */
public inline fun <K : Entity<K>, V, M : MutableMap<in K, in V>> EntitySequence<K, *>.associateWithTo(
    destination: M,
    valueSelector: (K) -> V
): M {
    for (element in this) destination.put(element, valueSelector(element))
    return destination
}

/**
 * Return an element at the given [index] or `null` if the [index] is out of bounds of this sequence.
 *
 * Especially, if a dialect is enabled, this function will use the pagination feature to obtain the very record only.
 * Assuming we are using MySQL and calling this function with an index 10, a SQL containing `limit 10, 1` will be
 * generated. But if there are no dialects enabled, then all records in the sequence will be obtained to ensure the
 * function just works.
 *
 * The operation is terminal.
 */
public fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.elementAtOrNull(index: Int): E? {
    try {
        @Suppress("UnconditionalJumpStatementInLoop")
        for (element in this.drop(index).take(1)) return element
        return null
    } catch (e: DialectFeatureNotSupportedException) {
        if (database.logger.isTraceEnabled()) {
            database.logger.trace("Pagination is not supported, retrieving all records instead. $e")
        }

        var count = 0
        for (element in this) {
            if (index == count++) return element
        }

        return null
    }
}

/**
 * Return an element at the given [index] or the result of calling the [defaultValue] function if the [index] is out
 * of bounds of this sequence.
 *
 * Especially, if a dialect is enabled, this function will use the pagination feature to obtain the very record only.
 * Assuming we are using MySQL and calling this function with an index 10, a SQL containing `limit 10, 1` will be
 * generated. But if there are no dialects enabled, then all records in the sequence will be obtained to ensure the
 * function just works.
 *
 * The operation is terminal.
 */
public inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.elementAtOrElse(
    index: Int,
    defaultValue: (Int) -> E
): E {
    return elementAtOrNull(index) ?: defaultValue(index)
}

/**
 * Return an element at the given [index] or throws an [IndexOutOfBoundsException] if the [index] is out of bounds
 * of this sequence.
 *
 * Especially, if a dialect is enabled, this function will use the pagination feature to obtain the very record only.
 * Assuming we are using MySQL and calling this function with an index 10, a SQL containing `limit 10, 1` will be
 * generated. But if there are no dialects enabled, then all records in the sequence will be obtained to ensure the
 * function just works.
 *
 * The operation is terminal.
 */
public fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.elementAt(index: Int): E {
    val result = elementAtOrNull(index)
    return result ?: throw IndexOutOfBoundsException("Sequence doesn't contain element at index $index.")
}

/**
 * Return the first element, or `null` if the sequence is empty.
 *
 * Especially, if a dialect is enabled, this function will use the pagination feature to obtain the very record only.
 * Assuming we are using MySQL, a SQL containing `limit 0, 1` will be generated. But if there are no dialects enabled,
 * then all records in the sequence will be obtained to ensure the function just works.
 *
 * The operation is terminal.
 */
public fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.firstOrNull(): E? {
    return elementAtOrNull(0)
}

/**
 * Return the first element matching the given [predicate], or `null` if element was not found.
 *
 * Especially, if a dialect is enabled, this function will use the pagination feature to obtain the very record only.
 * Assuming we are using MySQL, a SQL containing `limit 0, 1` will be generated. But if there are no dialects enabled,
 * then all records in the sequence matching the given [predicate] will be obtained to ensure the function just works.
 *
 * The operation is terminal.
 */
public inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.firstOrNull(
    predicate: (T) -> ColumnDeclaring<Boolean>
): E? {
    return filter(predicate).elementAtOrNull(0)
}

/**
 * Return the first element, or throws [NoSuchElementException] if the sequence is empty.
 *
 * Especially, if a dialect is enabled, this function will use the pagination feature to obtain the very record only.
 * Assuming we are using MySQL, a SQL containing `limit 0, 1` will be generated. But if there are no dialects enabled,
 * then all records in the sequence will be obtained to ensure the function just works.
 *
 * The operation is terminal.
 */
public fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.first(): E {
    return firstOrNull() ?: throw NoSuchElementException("Sequence is empty.")
}

/**
 * Return the first element matching the given [predicate], or throws [NoSuchElementException] if element was not found.
 *
 * Especially, if a dialect is enabled, this function will use the pagination feature to obtain the very record only.
 * Assuming we are using MySQL, a SQL containing `limit 0, 1` will be generated. But if there are no dialects enabled,
 * then all records in the sequence matching the given [predicate] will be obtained to ensure the function just works.
 *
 * The operation is terminal.
 */
public inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.first(
    predicate: (T) -> ColumnDeclaring<Boolean>
): E {
    val result = firstOrNull(predicate)
    return result ?: throw NoSuchElementException("Sequence contains no elements matching the predicate")
}

/**
 * Return the last element, or `null` if the sequence is empty.
 *
 * The operation is terminal.
 */
public fun <E : Any> EntitySequence<E, *>.lastOrNull(): E? {
    var last: E? = null
    for (element in this) last = element
    return last
}

/**
 * Return the last element matching the given [predicate], or `null` if no such element was found.
 *
 * The operation is terminal.
 */
public inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.lastOrNull(
    predicate: (T) -> ColumnDeclaring<Boolean>
): E? {
    return filter(predicate).lastOrNull()
}

/**
 * Return the last element, or throws [NoSuchElementException] if the sequence is empty.
 *
 * The operation is terminal.
 */
public fun <E : Any> EntitySequence<E, *>.last(): E {
    return lastOrNull() ?: throw NoSuchElementException("Sequence is empty.")
}

/**
 * Return the last element matching the given [predicate], or throws [NoSuchElementException] if no such element found.
 *
 * The operation is terminal.
 */
public inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.last(
    predicate: (T) -> ColumnDeclaring<Boolean>
): E {
    val result = lastOrNull(predicate)
    return result ?: throw NoSuchElementException("Sequence contains no elements matching the predicate")
}

/**
 * Return the first element matching the given [predicate], or `null` if no such element was found.
 *
 * Especially, if a dialect is enabled, this function will use the pagination feature to obtain the very record only.
 * Assuming we are using MySQL, a SQL containing `limit 0, 1` will be generated. But if there are no dialects enabled,
 * then all records in the sequence matching the given [predicate] will be obtained to ensure the function just works.
 *
 * The operation is terminal.
 */
public inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.find(
    predicate: (T) -> ColumnDeclaring<Boolean>
): E? {
    return firstOrNull(predicate)
}

/**
 * Return the last element matching the given [predicate], or `null` if no such element was found.
 *
 * The operation is terminal.
 */
public inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.findLast(
    predicate: (T) -> ColumnDeclaring<Boolean>
): E? {
    return lastOrNull(predicate)
}

/**
 * Return single element, or `null` if the sequence is empty or has more than one element.
 *
 * The operation is terminal.
 */
public fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.singleOrNull(): E? {
    val iterator = iterator()
    if (!iterator.hasNext()) return null

    val single = iterator.next()
    return if (iterator.hasNext()) null else single
}

/**
 * Return the single element matching the given [predicate], or `null` if element was not found or more than one
 * element was found.
 *
 * The operation is terminal.
 */
public inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.singleOrNull(
    predicate: (T) -> ColumnDeclaring<Boolean>
): E? {
    return filter(predicate).singleOrNull()
}

/**
 * Return the single element, or throws an exception if the sequence is empty or has more than one element.
 *
 * The operation is terminal.
 */
public fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.single(): E {
    val iterator = iterator()
    if (!iterator.hasNext()) throw NoSuchElementException("Sequence is empty.")

    val single = iterator.next()
    if (iterator.hasNext()) throw IllegalArgumentException("Sequence has more than one element.")
    return single
}

/**
 * Return the single element matching the given [predicate], or throws exception if there is no or more than one
 * matching element.
 *
 * The operation is terminal.
 */
public inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.single(
    predicate: (T) -> ColumnDeclaring<Boolean>
): E {
    return filter(predicate).single()
}

/**
 * Accumulate value starting with [initial] value and applying [operation] from left to right to current accumulator
 * value and each element.
 *
 * The operation is terminal.
 */
public inline fun <E : Any, R> EntitySequence<E, *>.fold(initial: R, operation: (acc: R, E) -> R): R {
    var accumulator = initial
    for (element in this) accumulator = operation(accumulator, element)
    return accumulator
}

/**
 * Accumulate value starting with [initial] value and applying [operation] from left to right to current accumulator
 * value and each element with its index in the original sequence.
 *
 * The [operation] function takes the index of an element, current accumulator value and the element itself, and
 * calculates the next accumulator value.
 *
 * The operation is terminal.
 */
public inline fun <E : Any, R> EntitySequence<E, *>.foldIndexed(
    initial: R,
    operation: (index: Int, acc: R, E) -> R
): R {
    var index = 0
    var accumulator = initial
    for (element in this) accumulator = operation(index++, accumulator, element)
    return accumulator
}

/**
 * Accumulate value starting with the first element and applying [operation] from left to right to current accumulator
 * value and each element.
 *
 * Throws an exception if this sequence is empty. If the sequence can be empty in an expected way, please use
 * [reduceOrNull] instead. It returns `null` when its receiver is empty.
 *
 * The [operation] function takes the current accumulator value and an element, and calculates the next
 * accumulator value.
 *
 * The operation is terminal.
 */
public inline fun <E : Any> EntitySequence<E, *>.reduce(operation: (acc: E, E) -> E): E {
    return reduceOrNull(operation) ?: throw UnsupportedOperationException("Empty sequence can't be reduced.")
}

/**
 * Accumulate value starting with the first element and applying [operation] from left to right to current accumulator
 * value and each element with its index in the original sequence.
 *
 * Throws an exception if this sequence is empty. If the sequence can be empty in an expected way, please use
 * [reduceIndexedOrNull] instead. It returns `null` when its receiver is empty.
 *
 * The [operation] function takes the index of an element, current accumulator value and the element itself and
 * calculates the next accumulator value.
 *
 * The operation is terminal.
 */
public inline fun <E : Any> EntitySequence<E, *>.reduceIndexed(operation: (index: Int, acc: E, E) -> E): E {
    return reduceIndexedOrNull(operation) ?: throw UnsupportedOperationException("Empty sequence can't be reduced.")
}

/**
 * Accumulate value starting with the first element and applying [operation] from left to right to current accumulator
 * value and each element.
 *
 * Returns `null` if the sequence is empty.
 *
 * The [operation] function takes the current accumulator value and an element, and calculates the next
 * accumulator value.
 *
 * The operation is terminal.
 *
 * @since 3.1.0
 */
public inline fun <E : Any> EntitySequence<E, *>.reduceOrNull(operation: (acc: E, E) -> E): E? {
    val iterator = iterator()
    if (!iterator.hasNext()) return null

    var accumulator = iterator.next()
    while (iterator.hasNext()) {
        accumulator = operation(accumulator, iterator.next())
    }

    return accumulator
}

/**
 * Accumulate value starting with the first element and applying [operation] from left to right to current accumulator
 * value and each element with its index in the original sequence.
 *
 * Returns `null` if the sequence is empty.
 *
 * The [operation] function takes the index of an element, current accumulator value and the element itself and
 * calculates the next accumulator value.
 *
 * The operation is terminal.
 *
 * @since 3.1.0
 */
public inline fun <E : Any> EntitySequence<E, *>.reduceIndexedOrNull(operation: (index: Int, acc: E, E) -> E): E? {
    var index = 1
    return reduceOrNull { acc, e -> operation(index++, acc, e) }
}

/**
 * Perform the given [action] on each element.
 *
 * The operation is terminal.
 */
public inline fun <E : Any> EntitySequence<E, *>.forEach(action: (E) -> Unit) {
    for (element in this) action(element)
}

/**
 * Perform the given [action] on each element, providing sequential index with the element.
 *
 * The [action] function takes the index of an element and the element itself and perform on the element.
 *
 * The operation is terminal.
 */
public inline fun <E : Any> EntitySequence<E, *>.forEachIndexed(action: (index: Int, E) -> Unit) {
    var index = 0
    for (element in this) action(index++, element)
}

/**
 * Return a lazy [Sequence] that wraps each element of the original sequence into an [IndexedValue] containing
 * the index of that element and the element itself.
 *
 * @since 3.0.0
 */
public fun <E : Any> EntitySequence<E, *>.withIndex(): Sequence<IndexedValue<E>> {
    return Sequence { IndexingIterator(iterator()) }
}

/**
 * Group elements of the original sequence by the key returned by the given [keySelector] function applied to each
 * element and return a map where each group key is associated with a list of corresponding elements.
 *
 * The returned map preserves the entry iteration order of the keys produced from the original sequence.
 *
 * The operation is terminal.
 */
public inline fun <E : Any, K> EntitySequence<E, *>.groupBy(keySelector: (E) -> K): Map<K, List<E>> {
    return groupByTo(LinkedHashMap(), keySelector)
}

/**
 * Group values returned by the [valueTransform] function applied to each element of the original sequence by the key
 * returned by the given [keySelector] function applied to the element and returns a map where each group key is
 * associated with a list of corresponding values.
 *
 * The returned map preserves the entry iteration order of the keys produced from the original sequence.
 *
 * The operation is terminal.
 */
public inline fun <E : Any, K, V> EntitySequence<E, *>.groupBy(
    keySelector: (E) -> K,
    valueTransform: (E) -> V
): Map<K, List<V>> {
    return groupByTo(LinkedHashMap(), keySelector, valueTransform)
}

/**
 * Group elements of the original sequence by the key returned by the given [keySelector] function applied to each
 * element and put to the [destination] map each group key associated with a list of corresponding elements.
 *
 * The operation is terminal.
 */
public inline fun <E : Any, K, M : MutableMap<in K, MutableList<E>>> EntitySequence<E, *>.groupByTo(
    destination: M,
    keySelector: (E) -> K
): M {
    for (element in this) {
        val key = keySelector(element)
        val list = destination.getOrPut(key) { ArrayList() }
        list += element
    }

    return destination
}

/**
 * Group values returned by the [valueTransform] function applied to each element of the original sequence by the key
 * returned by the given [keySelector] function applied to the element and put to the [destination] map each group key
 * associated with a list of corresponding values.
 *
 * The operation is terminal.
 */
public inline fun <E : Any, K, V, M : MutableMap<in K, MutableList<V>>> EntitySequence<E, *>.groupByTo(
    destination: M,
    keySelector: (E) -> K,
    valueTransform: (E) -> V
): M {
    for (element in this) {
        val key = keySelector(element)
        val list = destination.getOrPut(key) { ArrayList() }
        list += valueTransform(element)
    }

    return destination
}

/**
 * Create an [EntityGrouping] from the sequence to be used later with one of group-and-fold operations.
 *
 * The [keySelector] can be applied to each record to get its key, or used as the `group by` clause of generated SQLs.
 *
 * The operation is intermediate.
 */
public fun <E : Any, T : BaseTable<E>, K : Any> EntitySequence<E, T>.groupingBy(
    keySelector: (T) -> ColumnDeclaring<K>
): EntityGrouping<E, T, K> {
    return EntityGrouping(this, keySelector)
}

/**
 * Append the string from all the elements separated using [separator] and using the given [prefix] and [postfix].
 *
 * If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first
 * [limit] elements will be appended, followed by the [truncated] string (which defaults to "...").
 *
 * The operation is terminal.
 */
public fun <E : Any, A : Appendable> EntitySequence<E, *>.joinTo(
    buffer: A,
    separator: CharSequence = ", ",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    limit: Int = -1,
    truncated: CharSequence = "...",
    transform: ((E) -> CharSequence)? = null
): A {
    buffer.append(prefix)
    var count = 0
    for (element in this) {
        if (++count > 1) buffer.append(separator)
        if (limit < 0 || count <= limit) {
            if (transform != null) buffer.append(transform(element)) else buffer.append(element.toString())
        } else {
            buffer.append(truncated)
            break
        }
    }
    buffer.append(postfix)
    return buffer
}

/**
 * Create a string from all the elements separated using [separator] and using the given [prefix] and [postfix].
 *
 * If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first
 * [limit] elements will be appended, followed by the [truncated] string (which defaults to "...").
 *
 * The operation is terminal.
 */
public fun <E : Any> EntitySequence<E, *>.joinToString(
    separator: CharSequence = ", ",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    limit: Int = -1,
    truncated: CharSequence = "...",
    transform: ((E) -> CharSequence)? = null
): String {
    return joinTo(StringBuilder(), separator, prefix, postfix, limit, truncated, transform).toString()
}
