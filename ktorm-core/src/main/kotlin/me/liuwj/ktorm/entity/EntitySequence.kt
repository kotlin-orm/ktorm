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

package me.liuwj.ktorm.entity

import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.DialectFeatureNotSupportedException
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.expression.OrderByExpression
import me.liuwj.ktorm.expression.SelectExpression
import me.liuwj.ktorm.schema.BaseTable
import me.liuwj.ktorm.schema.Column
import me.liuwj.ktorm.schema.ColumnDeclaring
import java.sql.ResultSet
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.min

/**
 * Represents a sequence of entity objects. As the name implies, the style and use pattern of Ktorm's entity sequence
 * APIs are highly similar to [kotlin.sequences.Sequence] and the extension functions in Kotlin standard lib, as it
 * provides many extension functions with the same names, such as [filter], [map], [reduce], etc.
 *
 * To create an [EntitySequence], we can call one of the extension functions on a table object:
 * - [BaseTable.asSequence]
 * - [BaseTable.asSequenceWithoutReferences]
 *
 * This class wraps a [Query] object, and it’s iterator exactly wraps the query’s iterator. While an entity sequence is
 * iterated, its internal query is executed, and the [entityExtractor] function is applied to create an entity object
 * for each row. Here, the extractor might be [BaseTable.createEntity] or [BaseTable.createEntityWithoutReferences],
 * that depends on the arguments used to create sequence objects.
 *
 * Most of the entity sequence APIs are provided as extension functions, which can be divided into two groups:
 *
 * - **Intermediate operations:** these functions don’t execute the internal queries but return new-created sequence
 * objects applying some modifications. For example, the [filter] function creates a new sequence object with the filter
 * condition given by its parameter. The return types of intermediate operations are usually [EntitySequence], so we
 * can chaining call other sequence functions continuously.
 *
 * - **Terminal operations:** the return types of these functions are usually a collection or a computed result, as
 * they execute the queries right now, obtain their results and perform some calculations on them. Eg. [toList],
 * [reduce], etc.
 */
data class EntitySequence<E : Any, T : BaseTable<E>>(

    /**
     * The [Database] instance that the internal query is running on.
     */
    val database: Database,

    /**
     * The source table from which elements are obtained.
     */
    val sourceTable: T,

    /**
     * The SQL expression to be executed by this sequence when obtaining elements.
     */
    val expression: SelectExpression,

    /**
     * The function used to extract entity objects for each result row.
     */
    val entityExtractor: (row: QueryRowSet) -> E
) {
    /**
     * The internal query of this sequence to be executed, created by [expression].
     */
    val query = Query(database, expression)

    /**
     * The executable SQL string of the internal query.
     *
     * This property is delegated to [Query.sql], more details can be found in its documentation.
     */
    val sql get() = query.sql

    /**
     * The [ResultSet] object of the internal query, lazy initialized after first access, obtained from the database by
     * executing the generated SQL.
     *
     * This property is delegated to [Query.rowSet], more details can be found in its documentation.
     */
    val rowSet get() = query.rowSet

    /**
     * The total records count of this query ignoring the pagination params.
     *
     * This property is delegated to [Query.totalRecords], more details can be found in its documentation.
     */
    val totalRecords get() = query.totalRecords

    /**
     * Create a [kotlin.sequences.Sequence] instance that wraps this original entity sequence returning all the
     * elements when being iterated.
     */
    fun asKotlinSequence() = Sequence { iterator() }

    /**
     * Return an iterator over the elements of this sequence.
     */
    @Suppress("IteratorNotThrowingNoSuchElementException")
    operator fun iterator() = object : Iterator<E> {
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
 * Create an [EntitySequence], auto left joining all the reference tables.
 */
@Suppress("DEPRECATION")
@Deprecated(
    message = "This function will be removed in the future. Please use database.sequenceOf(..) instead.",
    replaceWith = ReplaceWith("database.sequenceOf(this)")
)
fun <E : Any, T : BaseTable<E>> T.asSequence(): EntitySequence<E, T> {
    return Database.global.sequenceOf(this)
}

/**
 * Create an [EntitySequence] without left joining reference tables.
 */
@Suppress("DEPRECATION")
@Deprecated(
    message = "This function will be removed in the future. Use database.sequenceOf(.., withReferences=false) instead.",
    replaceWith = ReplaceWith("database.sequenceOf(this, withReferences = false)")
)
fun <E : Any, T : BaseTable<E>> T.asSequenceWithoutReferences(): EntitySequence<E, T> {
    return Database.global.sequenceOf(this, withReferences = false)
}

/**
 * Create an [EntitySequence] from the specific table.
 */
fun <E : Any, T : BaseTable<E>> Database.sequenceOf(table: T, withReferences: Boolean = true): EntitySequence<E, T> {
    val query = if (withReferences) from(table).joinReferencesAndSelect() else from(table).select(table.columns)
    val entityExtractor = { row: QueryRowSet -> table.createEntity(row, withReferences) }
    return EntitySequence(this, table, query.expression as SelectExpression, entityExtractor)
}

/**
 * Append all elements to the given [destination] collection.
 *
 * The operation is terminal.
 */
fun <E : Any, C : MutableCollection<in E>> EntitySequence<E, *>.toCollection(destination: C): C {
    return asKotlinSequence().toCollection(destination)
}

/**
 * Return a [List] containing all the elements of this sequence.
 *
 * The operation is terminal.
 */
fun <E : Any> EntitySequence<E, *>.toList(): List<E> {
    return asKotlinSequence().toList()
}

/**
 * Return a [MutableList] containing all the elements of this sequence.
 *
 * The operation is terminal.
 */
fun <E : Any> EntitySequence<E, *>.toMutableList(): MutableList<E> {
    return asKotlinSequence().toMutableList()
}

/**
 * Return a [Set] containing all the elements of this sequence.
 *
 * The returned set preserves the element iteration order of the original sequence.
 *
 * The operation is terminal.
 */
fun <E : Any> EntitySequence<E, *>.toSet(): Set<E> {
    return asKotlinSequence().toSet()
}

/**
 * Return a [MutableSet] containing all the elements of this sequence.
 *
 * The returned set preserves the element iteration order of the original sequence.
 *
 * The operation is terminal.
 */
fun <E : Any> EntitySequence<E, *>.toMutableSet(): MutableSet<E> {
    return asKotlinSequence().toMutableSet()
}

/**
 * Return a [HashSet] containing all the elements of this sequence.
 *
 * The operation is terminal.
 */
fun <E : Any> EntitySequence<E, *>.toHashSet(): HashSet<E> {
    return asKotlinSequence().toHashSet()
}

/**
 * Return a [SortedSet] containing all the elements of this sequence.
 *
 * The operation is terminal.
 */
fun <E> EntitySequence<E, *>.toSortedSet(): SortedSet<E> where E : Any, E : Comparable<E> {
    return asKotlinSequence().toSortedSet()
}

/**
 * Return a [SortedSet] containing all the elements of this sequence.
 *
 * Elements in the set returned are sorted according to the given [comparator].
 *
 * The operation is terminal.
 */
fun <E> EntitySequence<E, *>.toSortedSet(
    comparator: Comparator<in E>
): SortedSet<E> where E : Any, E : Comparable<E> {
    return asKotlinSequence().toSortedSet(comparator)
}

/**
 * Return a sequence customizing the selected columns of the internal query.
 *
 * The operation is intermediate.
 */
inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.filterColumns(
    selector: (T) -> List<Column<*>>
): EntitySequence<E, T> {
    val columns = selector(sourceTable)
    if (columns.isEmpty()) {
        return this
    } else {
        return this.copy(expression = expression.copy(columns = columns.map { it.aliased(it.label) }))
    }
}

/**
 * Return a sequence containing only elements matching the given [predicate].
 *
 * The operation is intermediate.
 */
inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.filter(
    predicate: (T) -> ColumnDeclaring<Boolean>
): EntitySequence<E, T> {
    if (expression.where == null) {
        return this.copy(expression = expression.copy(where = predicate(sourceTable).asExpression()))
    } else {
        return this.copy(expression = expression.copy(where = expression.where and predicate(sourceTable)))
    }
}

/**
 * Return a sequence containing only elements not matching the given [predicate].
 *
 * The operation is intermediate.
 */
inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.filterNot(
    predicate: (T) -> ColumnDeclaring<Boolean>
): EntitySequence<E, T> {
    return filter { !predicate(it) }
}

/**
 * Append all elements matching the given [predicate] to the given [destination].
 *
 * The operation is terminal.
 */
inline fun <E : Any, T : BaseTable<E>, C : MutableCollection<in E>> EntitySequence<E, T>.filterTo(
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
inline fun <E : Any, T : BaseTable<E>, C : MutableCollection<in E>> EntitySequence<E, T>.filterNotTo(
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
inline fun <E : Any, R> EntitySequence<E, *>.map(transform: (E) -> R): List<R> {
    return mapTo(ArrayList(), transform)
}

/**
 * Apply the given [transform] function to each element of the original sequence
 * and append the results to the given [destination].
 *
 * The operation is terminal.
 */
inline fun <E : Any, R, C : MutableCollection<in R>> EntitySequence<E, *>.mapTo(
    destination: C,
    transform: (E) -> R
): C {
    for (item in this) destination += transform(item)
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
inline fun <E : Any, R> EntitySequence<E, *>.mapIndexed(transform: (index: Int, E) -> R): List<R> {
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
inline fun <E : Any, R, C : MutableCollection<in R>> EntitySequence<E, *>.mapIndexedTo(
    destination: C,
    transform: (index: Int, E) -> R
): C {
    var index = 0
    return mapTo(destination) { transform(index++, it) }
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
 * Ktorm also supports selecting two or more columns, we can change to [mapColumns2] or [mapColumns3], then we need
 * to wrap our selected columns by [Pair] or [Triple] in the closure, and the function’s return type becomes
 * `List<Pair<C1?, C2?>>` or `List<Triple<C1?, C2?, C3?>>`.
 *
 * The operation is terminal.
 *
 * @param isDistinct specify if the query is distinct, the generated SQL becomes `select distinct` if it's set to true.
 * @param columnSelector a function in which we should return a column or expression to be selected.
 * @return a list of the query results.
 */
inline fun <E : Any, T : BaseTable<E>, C : Any> EntitySequence<E, T>.mapColumns(
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
 * Ktorm also supports selecting two or more columns, we can change to [mapColumns2To] or [mapColumns3To], then we need
 * to wrap our selected columns by [Pair] or [Triple] in the closure, and the function’s return type becomes
 * `List<Pair<C1?, C2?>>` or `List<Triple<C1?, C2?, C3?>>`.
 *
 * The operation is terminal.
 *
 * @param destination a [MutableCollection] used to store the results.
 * @param isDistinct specify if the query is distinct, the generated SQL becomes `select distinct` if it's set to true.
 * @param columnSelector a function in which we should return a column or expression to be selected.
 * @return the [destination] collection of the query results.
 */
inline fun <E : Any, T : BaseTable<E>, C : Any, R : MutableCollection<in C?>> EntitySequence<E, T>.mapColumnsTo(
    destination: R,
    isDistinct: Boolean = false,
    columnSelector: (T) -> ColumnDeclaring<C>
): R {
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
inline fun <E : Any, T : BaseTable<E>, C : Any> EntitySequence<E, T>.mapColumnsNotNull(
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
inline fun <E : Any, T : BaseTable<E>, C : Any, R : MutableCollection<in C>> EntitySequence<E, T>.mapColumnsNotNullTo(
    destination: R,
    isDistinct: Boolean = false,
    columnSelector: (T) -> ColumnDeclaring<C>
): R {
    val column = columnSelector(sourceTable)

    val expr = expression.copy(
        columns = listOf(column.aliased(null)),
        isDistinct = isDistinct
    )

    return Query(database, expr).mapNotNullTo(destination) { row -> column.sqlType.getResult(row, 1) }
}

/**
 * Return a sequence customizing the `order by` clause of the internal query.
 *
 * The operation is intermediate.
 */
inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.sorted(
    selector: (T) -> List<OrderByExpression>
): EntitySequence<E, T> {
    return this.copy(expression = expression.copy(orderBy = selector(sourceTable)))
}

/**
 * Return a sequence sorting elements by the specific column in ascending order.
 *
 * The operation is intermediate.
 */
inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.sortedBy(
    selector: (T) -> ColumnDeclaring<*>
): EntitySequence<E, T> {
    return sorted { listOf(selector(it).asc()) }
}

/**
 * Return a sequence sorting elements by the specific column in descending order.
 *
 * The operation is intermediate.
 */
inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.sortedByDescending(
    selector: (T) -> ColumnDeclaring<*>
): EntitySequence<E, T> {
    return sorted { listOf(selector(it).desc()) }
}

/**
 * Returns a sequence containing all elements except first [n] elements.
 *
 * Note that this function is implemented based on the pagination feature of the specific databases. However, the SQL
 * standard doesn’t say how to implement paging queries, and different databases provide different implementations on
 * that. So we have to enable a dialect if we need to use this function, otherwise an exception will be thrown.
 *
 * The operation is intermediate.
 */
fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.drop(n: Int): EntitySequence<E, T> {
    if (n == 0) {
        return this
    } else {
        val offset = expression.offset ?: 0
        return this.copy(expression = expression.copy(offset = offset + n))
    }
}

/**
 * Returns a sequence containing first [n] elements.
 *
 * Note that this function is implemented based on the pagination feature of the specific databases. However, the SQL
 * standard doesn’t say how to implement paging queries, and different databases provide different implementations on
 * that. So we have to enable a dialect if we need to use this function, otherwise an exception will be thrown.
 *
 * The operation is intermediate.
 */
fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.take(n: Int): EntitySequence<E, T> {
    val limit = expression.limit ?: Int.MAX_VALUE
    return this.copy(expression = expression.copy(limit = min(limit, n)))
}

/**
 * Perform an aggregation given by [aggregationSelector] for all elements in the sequence,
 * and return the aggregate result.
 *
 * Ktorm also supports aggregating two or more columns, we can change to [EntitySequence.aggregateColumns2] or
 * [EntitySequence.aggregateColumns3], then we need to wrap our aggregate expressions by [Pair] or [Triple] in
 * the closure, and the function’s return type becomes `Pair<C1?, C2?>` or `Triple<C1?, C2?, C3?>`.
 *
 * The operation is terminal.
 *
 * @param aggregationSelector a function that accepts the source table and returns the aggregate expression.
 * @return the aggregate result.
 */
inline fun <E : Any, T : BaseTable<E>, C : Any> EntitySequence<E, T>.aggregateColumns(
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
fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.count(): Int {
    val count = aggregateColumns { me.liuwj.ktorm.dsl.count() }
    return count ?: error("Count expression returns null, which should never happens.")
}

/**
 * Return the number of elements matching the given [predicate].
 *
 * The operation is terminal.
 */
inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.count(
    predicate: (T) -> ColumnDeclaring<Boolean>
): Int {
    return filter(predicate).count()
}

/**
 * Return `true` if the sequence has no elements.
 *
 * The operation is terminal.
 */
fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.isEmpty(): Boolean {
    return count() == 0
}

/**
 * Return `true` if the sequence has at lease one element.
 *
 * The operation is terminal.
 */
fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.isNotEmpty(): Boolean {
    return count() > 0
}

/**
 * Return `true` if the sequence has no elements.
 *
 * The operation is terminal.
 */
fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.none(): Boolean {
    return count() == 0
}

/**
 * Return `true` if no elements match the given [predicate].
 *
 * The operation is terminal.
 */
inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.none(
    predicate: (T) -> ColumnDeclaring<Boolean>
): Boolean {
    return count(predicate) == 0
}

/**
 * Return `true` if the sequence has at lease one element.
 *
 * The operation is terminal.
 */
fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.any(): Boolean {
    return count() > 0
}

/**
 * Return `true` if at least one element matches the given [predicate].
 *
 * The operation is terminal.
 */
inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.any(
    predicate: (T) -> ColumnDeclaring<Boolean>
): Boolean {
    return count(predicate) > 0
}

/**
 * Return `true` if all elements match the given [predicate].
 *
 * The operation is terminal.
 */
inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.all(
    predicate: (T) -> ColumnDeclaring<Boolean>
): Boolean {
    return none { !predicate(it) }
}

/**
 * Return the sum of the column given by [selector] in this sequence.
 *
 * The operation is terminal.
 */
inline fun <E : Any, T : BaseTable<E>, C : Number> EntitySequence<E, T>.sumBy(
    selector: (T) -> ColumnDeclaring<C>
): C? {
    return aggregateColumns { sum(selector(it)) }
}

/**
 * Return the max value of the column given by [selector] in this sequence.
 *
 * The operation is terminal.
 */
inline fun <E : Any, T : BaseTable<E>, C : Comparable<C>> EntitySequence<E, T>.maxBy(
    selector: (T) -> ColumnDeclaring<C>
): C? {
    return aggregateColumns { max(selector(it)) }
}

/**
 * Return the min value of the column given by [selector] in this sequence.
 *
 * The operation is terminal.
 */
inline fun <E : Any, T : BaseTable<E>, C : Comparable<C>> EntitySequence<E, T>.minBy(
    selector: (T) -> ColumnDeclaring<C>
): C? {
    return aggregateColumns { min(selector(it)) }
}

/**
 * Return the average value of the column given by [selector] in this sequence.
 *
 * The operation is terminal.
 */
inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.averageBy(
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
inline fun <E : Any, K, V> EntitySequence<E, *>.associate(
    transform: (E) -> Pair<K, V>
): Map<K, V> {
    return asKotlinSequence().associate(transform)
}

/**
 * Return a [Map] containing the elements from the given sequence indexed by the key returned from [keySelector]
 * function applied to each element.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original sequence.
 *
 * The operation is terminal.
 */
inline fun <E : Any, K> EntitySequence<E, *>.associateBy(
    keySelector: (E) -> K
): Map<K, E> {
    return asKotlinSequence().associateBy(keySelector)
}

/**
 * Return a [Map] containing the values provided by [valueTransform] and indexed by [keySelector] functions
 * applied to elements of the given sequence.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original sequence.
 *
 * The operation is terminal.
 */
inline fun <E : Any, K, V> EntitySequence<E, *>.associateBy(
    keySelector: (E) -> K,
    valueTransform: (E) -> V
): Map<K, V> {
    return asKotlinSequence().associateBy(keySelector, valueTransform)
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
inline fun <K : Entity<K>, V> EntitySequence<K, *>.associateWith(
    valueSelector: (K) -> V
): Map<K, V> {
    return asKotlinSequence().associateWith(valueSelector)
}

/**
 * Populate and return the [destination] mutable map with key-value pairs provided by [transform] function applied
 * to each element of the given sequence.
 *
 * If any of two pairs would have the same key the last one gets added to the map.
 *
 * The operation is terminal.
 */
inline fun <E : Any, K, V, M : MutableMap<in K, in V>> EntitySequence<E, *>.associateTo(
    destination: M,
    transform: (E) -> Pair<K, V>
): M {
    return asKotlinSequence().associateTo(destination, transform)
}

/**
 * Populate and return the [destination] mutable map with key-value pairs, where key is provided by the [keySelector]
 * function applied to each element of the given sequence and value is the element itself.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The operation is terminal.
 */
inline fun <E : Any, K, M : MutableMap<in K, in E>> EntitySequence<E, *>.associateByTo(
    destination: M,
    keySelector: (E) -> K
): M {
    return asKotlinSequence().associateByTo(destination, keySelector)
}

/**
 * Populate and return the [destination] mutable map with key-value pairs, where key is provided by the [keySelector]
 * function and and value is provided by the [valueTransform] function applied to elements of the given sequence.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The operation is terminal.
 */
inline fun <E : Any, K, V, M : MutableMap<in K, in V>> EntitySequence<E, *>.associateByTo(
    destination: M,
    keySelector: (E) -> K,
    valueTransform: (E) -> V
): M {
    return asKotlinSequence().associateByTo(destination, keySelector, valueTransform)
}

/**
 * Populate and return the [destination] mutable map with key-value pairs for each element of the given sequence,
 * where key is the element itself and value is provided by the [valueSelector] function applied to that key.
 *
 * If any two elements are equal, the last one overwrites the former value in the map.
 *
 * The operation is terminal.
 */
inline fun <K : Entity<K>, V, M : MutableMap<in K, in V>> EntitySequence<K, *>.associateWithTo(
    destination: M,
    valueSelector: (K) -> V
): M {
    return asKotlinSequence().associateWithTo(destination, valueSelector)
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
fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.elementAtOrNull(index: Int): E? {
    try {
        return drop(index).take(1).asKotlinSequence().firstOrNull()
    } catch (e: DialectFeatureNotSupportedException) {
        if (database.logger != null && database.logger.isTraceEnabled()) {
            database.logger.trace("Pagination is not supported, retrieving all records instead: ", e)
        }

        return asKotlinSequence().elementAtOrNull(index)
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
inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.elementAtOrElse(
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
fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.elementAt(index: Int): E {
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
fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.firstOrNull(): E? {
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
inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.firstOrNull(
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
fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.first(): E {
    return elementAt(0)
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
inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.first(predicate: (T) -> ColumnDeclaring<Boolean>): E {
    return filter(predicate).elementAt(0)
}

/**
 * Return the last element, or `null` if the sequence is empty.
 *
 * The operation is terminal.
 */
fun <E : Any> EntitySequence<E, *>.lastOrNull(): E? {
    return asKotlinSequence().lastOrNull()
}

/**
 * Return the last element matching the given [predicate], or `null` if no such element was found.
 *
 * The operation is terminal.
 */
inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.lastOrNull(
    predicate: (T) -> ColumnDeclaring<Boolean>
): E? {
    return filter(predicate).lastOrNull()
}

/**
 * Return the last element, or throws [NoSuchElementException] if the sequence is empty.
 *
 * The operation is terminal.
 */
fun <E : Any> EntitySequence<E, *>.last(): E {
    return lastOrNull() ?: throw NoSuchElementException("Sequence is empty.")
}

/**
 * Return the last element matching the given [predicate], or throws [NoSuchElementException] if no such element found.
 *
 * The operation is terminal.
 */
inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.last(predicate: (T) -> ColumnDeclaring<Boolean>): E {
    return filter(predicate).last()
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
inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.find(predicate: (T) -> ColumnDeclaring<Boolean>): E? {
    return firstOrNull(predicate)
}

/**
 * Return the last element matching the given [predicate], or `null` if no such element was found.
 *
 * The operation is terminal.
 */
inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.findLast(predicate: (T) -> ColumnDeclaring<Boolean>): E? {
    return lastOrNull(predicate)
}

/**
 * Return single element, or `null` if the sequence is empty or has more than one element.
 *
 * The operation is terminal.
 */
fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.singleOrNull(): E? {
    return asKotlinSequence().singleOrNull()
}

/**
 * Return the single element matching the given [predicate], or `null` if element was not found or more than one
 * element was found.
 *
 * The operation is terminal.
 */
inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.singleOrNull(
    predicate: (T) -> ColumnDeclaring<Boolean>
): E? {
    return filter(predicate).singleOrNull()
}

/**
 * Return the single element, or throws an exception if the sequence is empty or has more than one element.
 *
 * The operation is terminal.
 */
fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.single(): E {
    return asKotlinSequence().single()
}

/**
 * Return the single element matching the given [predicate], or throws exception if there is no or more than one
 * matching element.
 *
 * The operation is terminal.
 */
inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.single(predicate: (T) -> ColumnDeclaring<Boolean>): E {
    return filter(predicate).single()
}

/**
 * Accumulate value starting with [initial] value and applying [operation] from left to right to current accumulator
 * value and each element.
 *
 * The operation is terminal.
 */
inline fun <E : Any, R> EntitySequence<E, *>.fold(initial: R, operation: (acc: R, E) -> R): R {
    return asKotlinSequence().fold(initial, operation)
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
inline fun <E : Any, R> EntitySequence<E, *>.foldIndexed(initial: R, operation: (index: Int, acc: R, E) -> R): R {
    return asKotlinSequence().foldIndexed(initial, operation)
}

/**
 * Accumulate value starting with the first element and applying [operation] from left to right to current accumulator
 * value and each element.
 *
 * The operation is terminal.
 */
inline fun <E : Any> EntitySequence<E, *>.reduce(operation: (acc: E, E) -> E): E {
    return asKotlinSequence().reduce(operation)
}

/**
 * Accumulate value starting with the first element and applying [operation] from left to right to current accumulator
 * value and each element with its index in the original sequence.
 *
 * The [operation] function takes the index of an element, current accumulator value and the element itself and
 * calculates the next accumulator value.
 *
 * The operation is terminal.
 */
inline fun <E : Any> EntitySequence<E, *>.reduceIndexed(operation: (index: Int, acc: E, E) -> E): E {
    return asKotlinSequence().reduceIndexed(operation)
}

/**
 * Perform the given [action] on each element.
 *
 * The operation is terminal.
 */
inline fun <E : Any> EntitySequence<E, *>.forEach(action: (E) -> Unit) {
    for (item in this) action(item)
}

/**
 * Perform the given [action] on each element, providing sequential index with the element.
 *
 * The [action] function takes the index of an element and the element itself and perform on the element.
 *
 * The operation is terminal.
 */
inline fun <E : Any> EntitySequence<E, *>.forEachIndexed(action: (index: Int, E) -> Unit) {
    var index = 0
    for (item in this) action(index++, item)
}

/**
 * Group elements of the original sequence by the key returned by the given [keySelector] function applied to each
 * element and return a map where each group key is associated with a list of corresponding elements.
 *
 * The returned map preserves the entry iteration order of the keys produced from the original sequence.
 *
 * The operation is terminal.
 */
inline fun <E : Any, K> EntitySequence<E, *>.groupBy(
    keySelector: (E) -> K
): Map<K, List<E>> {
    return asKotlinSequence().groupBy(keySelector)
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
inline fun <E : Any, K, V> EntitySequence<E, *>.groupBy(
    keySelector: (E) -> K,
    valueTransform: (E) -> V
): Map<K, List<V>> {
    return asKotlinSequence().groupBy(keySelector, valueTransform)
}

/**
 * Group elements of the original sequence by the key returned by the given [keySelector] function applied to each
 * element and put to the [destination] map each group key associated with a list of corresponding elements.
 *
 * The operation is terminal.
 */
inline fun <E : Any, K, M : MutableMap<in K, MutableList<E>>> EntitySequence<E, *>.groupByTo(
    destination: M,
    keySelector: (E) -> K
): M {
    return asKotlinSequence().groupByTo(destination, keySelector)
}

/**
 * Group values returned by the [valueTransform] function applied to each element of the original sequence by the key
 * returned by the given [keySelector] function applied to the element and put to the [destination] map each group key
 * associated with a list of corresponding values.
 *
 * The operation is terminal.
 */
inline fun <E : Any, K, V, M : MutableMap<in K, MutableList<V>>> EntitySequence<E, *>.groupByTo(
    destination: M,
    keySelector: (E) -> K,
    valueTransform: (E) -> V
): M {
    return asKotlinSequence().groupByTo(destination, keySelector, valueTransform)
}

/**
 * Create an [EntityGrouping] from the sequence to be used later with one of group-and-fold operations.
 *
 * The [keySelector] can be applied to each record to get its key, or used as the `group by` clause of generated SQLs.
 *
 * The operation is intermediate.
 */
fun <E : Any, T : BaseTable<E>, K : Any> EntitySequence<E, T>.groupingBy(
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
fun <E : Any, A : Appendable> EntitySequence<E, *>.joinTo(
    buffer: A,
    separator: CharSequence = ", ",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    limit: Int = -1,
    truncated: CharSequence = "...",
    transform: ((E) -> CharSequence)? = null
): A {
    return asKotlinSequence().joinTo(buffer, separator, prefix, postfix, limit, truncated, transform)
}

/**
 * Create a string from all the elements separated using [separator] and using the given [prefix] and [postfix].
 *
 * If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first
 * [limit] elements will be appended, followed by the [truncated] string (which defaults to "...").
 *
 * The operation is terminal.
 */
fun <E : Any> EntitySequence<E, *>.joinToString(
    separator: CharSequence = ", ",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    limit: Int = -1,
    truncated: CharSequence = "...",
    transform: ((E) -> CharSequence)? = null
): String {
    return asKotlinSequence().joinToString(separator, prefix, postfix, limit, truncated, transform)
}
