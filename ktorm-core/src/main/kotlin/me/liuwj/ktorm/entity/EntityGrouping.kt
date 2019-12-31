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

import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.schema.BaseTable
import me.liuwj.ktorm.schema.ColumnDeclaring

/**
 * Wraps an [EntitySequence] with a [keySelector] function, which can be applied to each record to get its key,
 * or used as the `group by` clause of the generated SQL.
 *
 * An [EntityGrouping] structure serves as an intermediate step in group-and-fold operations: they group elements
 * by their keys and then fold each group with some aggregation operation.
 *
 * Entity groups are created by attaching `keySelector: (T) -> ColumnDeclaring<K>` function to an entity sequence.
 * To get an instance of [EntityGrouping], use the extension function [EntitySequence.groupingBy].
 *
 * For the list of group-and-fold operations available, see the extension functions below.
 *
 * @property sequence the source entity sequence of this grouping.
 * @property keySelector a function used to extract the key of a record in the source table.
 */
data class EntityGrouping<E : Any, T : BaseTable<E>, K : Any>(
    val sequence: EntitySequence<E, T>,
    val keySelector: (T) -> ColumnDeclaring<K>
) {
    /**
     * Create a [kotlin.collections.Grouping] instance that wraps this original entity grouping returning all the
     * elements in the source sequence when being iterated.
     */
    fun asKotlinGrouping() = object : Grouping<E, K?> {
        private val allEntities = LinkedHashMap<E, K?>()

        init {
            val keyColumn = keySelector(sequence.sourceTable)
            val expr = sequence.expression.copy(
                columns = sequence.expression.columns + keyColumn.aliased("_group_key")
            )

            for (row in Query(expr)) {
                val entity = sequence.sourceTable.createEntity(row)
                val groupKey = keyColumn.sqlType.getResult(row, expr.columns.size)
                allEntities[entity] = groupKey
            }
        }

        override fun sourceIterator(): Iterator<E> {
            return allEntities.keys.iterator()
        }

        override fun keyOf(element: E): K? {
            return allEntities[element]
        }
    }
}

/**
 * Group elements from the source sequence by key and perform the given aggregation for elements in each group,
 * then store the results in a new [Map].
 *
 * The key for each group is provided by the [EntityGrouping.keySelector] function, and the generated SQL is like:
 * `select key, aggregation from source group by key`.
 *
 * Ktorm also supports aggregating two or more columns, we can change to [EntityGrouping.aggregateColumns2] or
 * [EntityGrouping.aggregateColumns3], then we need to wrap our aggregate expressions by [Pair] or [Triple] in
 * the closure, and the function’s return type becomes `Map<K?, Pair<C1?, C2?>>` or `Map<K?, Triple<C1?, C2?, C3?>>`.
 *
 * @param aggregationSelector a function that accepts the source table and returns the aggregate expression.
 * @return a [Map] associating the key of each group with the result of aggregation of the group elements.
 */
inline fun <E : Any, T : BaseTable<E>, K, C> EntityGrouping<E, T, K>.aggregateColumns(
    aggregationSelector: (T) -> ColumnDeclaring<C>
): Map<K?, C?> where K : Any, C : Any {
    return aggregateColumnsTo(LinkedHashMap(), aggregationSelector)
}

/**
 * Group elements from the source sequence by key and perform the given aggregation for elements in each group,
 * then store the results in the [destination] map.
 *
 * The key for each group is provided by the [EntityGrouping.keySelector] function, and the generated SQL is like:
 * `select key, aggregation from source group by key`.
 *
 * Ktorm also supports aggregating two or more columns, we can change to [EntityGrouping.aggregateColumns2To] or
 * [EntityGrouping.aggregateColumns3To], then we need to wrap our aggregate expressions by [Pair] or [Triple] in
 * the closure, and the function’s return type becomes `Map<K?, Pair<C1?, C2?>>` or `Map<K?, Triple<C1?, C2?, C3?>>`.
 *
 * @param destination a [MutableMap] used to store the results.
 * @param aggregationSelector a function that accepts the source table and returns the aggregate expression.
 * @return the [destination] map associating the key of each group with the result of aggregation of the group elements.
 */
inline fun <E : Any, T : BaseTable<E>, K, C, M> EntityGrouping<E, T, K>.aggregateColumnsTo(
    destination: M,
    aggregationSelector: (T) -> ColumnDeclaring<C>
): M where K : Any, C : Any, M : MutableMap<in K?, in C?> {
    val keyColumn = keySelector(sequence.sourceTable)
    val aggregation = aggregationSelector(sequence.sourceTable)

    val expr = sequence.expression.copy(
        columns = listOf(keyColumn, aggregation).map { it.aliased(null) },
        groupBy = listOf(keyColumn.asExpression())
    )

    for (row in Query(expr)) {
        val key = keyColumn.sqlType.getResult(row, 1)
        val value = aggregation.sqlType.getResult(row, 2)
        destination[key] = value
    }

    return destination
}

/**
 * Group elements from the source sequence by key and count elements in each group.
 *
 * The key for each group is provided by the [EntityGrouping.keySelector] function, and the generated SQL is like:
 * `select key, count(*) from source group by key`.
 *
 * @return a [Map] associating the key of each group with the count of elements in the group.
 */
fun <E : Any, T : BaseTable<E>, K> EntityGrouping<E, T, K>.eachCount(): Map<K?, Int> where K : Any {
    return eachCountTo(LinkedHashMap())
}

/**
 * Group elements from the source sequence by key and count elements in each group,
 * then store the results in the [destination] map.
 *
 * The key for each group is provided by the [EntityGrouping.keySelector] function, and the generated SQL is like:
 * `select key, count(*) from source group by key`.
 *
 * @param destination a [MutableMap] used to store the results.
 * @return the [destination] map associating the key of each group with the count of elements in the group.
 */
@Suppress("UNCHECKED_CAST")
fun <E : Any, T : BaseTable<E>, K, M> EntityGrouping<E, T, K>.eachCountTo(
    destination: M
): M where K : Any, M : MutableMap<in K?, Int> {
    return aggregateColumnsTo(destination as MutableMap<in K?, Int?>) { count() } as M
}

/**
 * Group elements from the source sequence by key and sum the columns or expressions provided by the [columnSelector]
 * function for elements in each group.
 *
 * The key for each group is provided by the [EntityGrouping.keySelector] function, and the generated SQL is like:
 * `select key, sum(column) from source group by key`.
 *
 * @param columnSelector a function that accepts the source table and returns the column or expression for summing.
 * @return a [Map] associating the key of each group with the summing result in the group.
 */
inline fun <E : Any, T : BaseTable<E>, K, C> EntityGrouping<E, T, K>.eachSumBy(
    columnSelector: (T) -> ColumnDeclaring<C>
): Map<K?, C?> where K : Any, C : Number {
    return eachSumByTo(LinkedHashMap(), columnSelector)
}

/**
 * Group elements from the source sequence by key and sum the columns or expressions provided by the [columnSelector]
 * function for elements in each group, then store the results in the [destination] map.
 *
 * The key for each group is provided by the [EntityGrouping.keySelector] function, and the generated SQL is like:
 * `select key, sum(column) from source group by key`.
 *
 * @param destination a [MutableMap] used to store the results.
 * @param columnSelector a function that accepts the source table and returns the column or expression for summing.
 * @return the [destination] map associating the key of each group with the summing result in the group.
 */
inline fun <E : Any, T : BaseTable<E>, K, C, M> EntityGrouping<E, T, K>.eachSumByTo(
    destination: M,
    columnSelector: (T) -> ColumnDeclaring<C>
): M where K : Any, C : Number, M : MutableMap<in K?, in C?> {
    return aggregateColumnsTo(destination) { sum(columnSelector(it)) }
}

/**
 * Group elements from the source sequence by key and get the max value of the columns or expressions provided by the
 * [columnSelector] function for elements in each group.
 *
 * The key for each group is provided by the [EntityGrouping.keySelector] function, and the generated SQL is like:
 * `select key, max(column) from source group by key`.
 *
 * @param columnSelector a function that accepts the source table and returns a column or expression.
 * @return a [Map] associating the key of each group with the max value in the group.
 */
inline fun <E : Any, T : BaseTable<E>, K, C> EntityGrouping<E, T, K>.eachMaxBy(
    columnSelector: (T) -> ColumnDeclaring<C>
): Map<K?, C?> where K : Any, C : Comparable<C> {
    return eachMaxByTo(LinkedHashMap(), columnSelector)
}

/**
 * Group elements from the source sequence by key and get the max value of the columns or expressions provided by the
 * [columnSelector] function for elements in each group, then store the results in the [destination] map.
 *
 * The key for each group is provided by the [EntityGrouping.keySelector] function, and the generated SQL is like:
 * `select key, max(column) from source group by key`.
 *
 * @param destination a [MutableMap] used to store the results.
 * @param columnSelector a function that accepts the source table and returns a column or expression.
 * @return a [destination] map associating the key of each group with the max value in the group.
 */
inline fun <E : Any, T : BaseTable<E>, K, C, M> EntityGrouping<E, T, K>.eachMaxByTo(
    destination: M,
    columnSelector: (T) -> ColumnDeclaring<C>
): M where K : Any, C : Comparable<C>, M : MutableMap<in K?, in C?> {
    return aggregateColumnsTo(destination) { max(columnSelector(it)) }
}

/**
 * Group elements from the source sequence by key and get the min value of the columns or expressions provided by the
 * [columnSelector] function for elements in each group.
 *
 * The key for each group is provided by the [EntityGrouping.keySelector] function, and the generated SQL is like:
 * `select key, min(column) from source group by key`.
 *
 * @param columnSelector a function that accepts the source table and returns a column or expression.
 * @return a [Map] associating the key of each group with the min value in the group.
 */
inline fun <E : Any, T : BaseTable<E>, K, C> EntityGrouping<E, T, K>.eachMinBy(
    columnSelector: (T) -> ColumnDeclaring<C>
): Map<K?, C?> where K : Any, C : Comparable<C> {
    return eachMinByTo(LinkedHashMap(), columnSelector)
}

/**
 * Group elements from the source sequence by key and get the min value of the columns or expressions provided by the
 * [columnSelector] function for elements in each group, then store the results in the [destination] map.
 *
 * The key for each group is provided by the [EntityGrouping.keySelector] function, and the generated SQL is like:
 * `select key, min(column) from source group by key`.
 *
 * @param destination a [MutableMap] used to store the results.
 * @param columnSelector a function that accepts the source table and returns a column or expression.
 * @return a [destination] map associating the key of each group with the min value in the group.
 */
inline fun <E : Any, T : BaseTable<E>, K, C, M> EntityGrouping<E, T, K>.eachMinByTo(
    destination: M,
    columnSelector: (T) -> ColumnDeclaring<C>
): M where K : Any, C : Comparable<C>, M : MutableMap<in K?, in C?> {
    return aggregateColumnsTo(destination) { min(columnSelector(it)) }
}

/**
 * Group elements from the source sequence by key and average the columns or expressions provided by the
 * [columnSelector] function for elements in each group.
 *
 * The key for each group is provided by the [EntityGrouping.keySelector] function, and the generated SQL is like:
 * `select key, avg(column) from source group by key`.
 *
 * @param columnSelector a function that accepts the source table and returns the column or expression for averaging.
 * @return a [Map] associating the key of each group with the averaging result in the group.
 */
inline fun <E : Any, T : BaseTable<E>, K> EntityGrouping<E, T, K>.eachAverageBy(
    columnSelector: (T) -> ColumnDeclaring<out Number>
): Map<K?, Double?> where K : Any {
    return eachAverageByTo(LinkedHashMap(), columnSelector)
}

/**
 * Group elements from the source sequence by key and average the columns or expressions provided by the
 * [columnSelector] function for elements in each group, then store the results in the [destination] map.
 *
 * The key for each group is provided by the [EntityGrouping.keySelector] function, and the generated SQL is like:
 * `select key, avg(column) from source group by key`.
 *
 * @param destination a [MutableMap] used to store the results.
 * @param columnSelector a function that accepts the source table and returns the column or expression for averaging.
 * @return the [destination] map associating the key of each group with the averaging result in the group.
 */
inline fun <E : Any, T : BaseTable<E>, K, M> EntityGrouping<E, T, K>.eachAverageByTo(
    destination: M,
    columnSelector: (T) -> ColumnDeclaring<out Number>
): M where K : Any, M : MutableMap<in K?, in Double?> {
    return aggregateColumnsTo(destination) { avg(columnSelector(it)) }
}

/**
 * Groups elements from the source sequence by key and applies [operation] to the elements of each group sequentially,
 * passing the previously accumulated value and the current element as arguments, and stores the results in a new map.
 *
 * This function is delegated to [Grouping.aggregate], more details can be found in its documentation.
 */
inline fun <E : Any, K : Any, R> EntityGrouping<E, *, K>.aggregate(
    operation: (key: K?, accumulator: R?, element: E, first: Boolean) -> R
): Map<K?, R> {
    return asKotlinGrouping().aggregate(operation)
}

/**
 * Groups elements from the source sequence by key and applies [operation] to the elements of each group sequentially,
 * passing the previously accumulated value and the current element as arguments, and stores the results in the given
 * [destination] map.
 *
 * This function is delegated to [Grouping.aggregateTo], more details can be found in its documentation.
 */
inline fun <E : Any, K : Any, R, M : MutableMap<in K?, R>> EntityGrouping<E, *, K>.aggregateTo(
    destination: M,
    operation: (key: K?, accumulator: R?, element: E, first: Boolean) -> R
): M {
    return asKotlinGrouping().aggregateTo(destination, operation)
}

/**
 * Groups elements from the source sequence by key and applies [operation] to the elements of each group sequentially,
 * passing the previously accumulated value and the current element as arguments, and stores the results in a new map.
 * An initial value of accumulator is provided by [initialValueSelector] function.
 *
 * This function is delegated to [Grouping.fold], more details can be found in its documentation.
 */
inline fun <E : Any, K : Any, R> EntityGrouping<E, *, K>.fold(
    initialValueSelector: (key: K?, element: E) -> R,
    operation: (key: K?, accumulator: R, element: E) -> R
): Map<K?, R> {
    return asKotlinGrouping().fold(initialValueSelector, operation)
}

/**
 * Groups elements from the source sequence by key and applies [operation] to the elements of each group sequentially,
 * passing the previously accumulated value and the current element as arguments, and stores the results in the given
 * [destination] map. An initial value of accumulator is provided by [initialValueSelector] function.
 *
 * This function is delegated to [Grouping.foldTo], more details can be found in its documentation.
 */
inline fun <E : Any, K : Any, R, M : MutableMap<in K?, R>> EntityGrouping<E, *, K>.foldTo(
    destination: M,
    initialValueSelector: (key: K?, element: E) -> R,
    operation: (key: K?, accumulator: R, element: E) -> R
): M {
    return asKotlinGrouping().foldTo(destination, initialValueSelector, operation)
}

/**
 * Groups elements from the source sequence by key and applies [operation] to the elements of each group sequentially,
 * passing the previously accumulated value and the current element as arguments, and stores the results in a new map.
 * An initial value of accumulator is the same [initialValue] for each group.
 *
 * This function is delegated to [Grouping.fold], more details can be found in its documentation.
 */
inline fun <E : Any, K : Any, R> EntityGrouping<E, *, K>.fold(
    initialValue: R,
    operation: (accumulator: R, element: E) -> R
): Map<K?, R> {
    return asKotlinGrouping().fold(initialValue, operation)
}

/**
 * Groups elements from the source sequence by key and applies [operation] to the elements of each group sequentially,
 * passing the previously accumulated value and the current element as arguments, and stores the results in the given
 * [destination] map. An initial value of accumulator is the same [initialValue] for each group.
 *
 * This function is delegated to [Grouping.foldTo], more details can be found in its documentation.
 */
inline fun <E : Any, K : Any, R, M : MutableMap<in K?, R>> EntityGrouping<E, *, K>.foldTo(
    destination: M,
    initialValue: R,
    operation: (accumulator: R, element: E) -> R
): M {
    return asKotlinGrouping().foldTo(destination, initialValue, operation)
}

/**
 * Groups elements from the source sequence by key and applies the reducing [operation] to the elements of each group
 * sequentially starting from the second element of the group, passing the previously accumulated value and the current
 * element as arguments, and stores the results in a new map. An initial value of accumulator is the first element of
 * the group.
 *
 * This function is delegated to [Grouping.reduce], more details can be found in its documentation.
 */
inline fun <E : Any, K : Any> EntityGrouping<E, *, K>.reduce(
    operation: (key: K?, accumulator: E, element: E) -> E
): Map<K?, E> {
    return asKotlinGrouping().reduce(operation)
}

/**
 * Groups elements from the source sequence by key and applies the reducing [operation] to the elements of each group
 * sequentially starting from the second element of the group, passing the previously accumulated value and the current
 * element as arguments, and stores the results in the given [destination] map. An initial value of accumulator is the
 * first element of the group.
 *
 * This function is delegated to [Grouping.reduceTo], more details can be found in its documentation.
 */
inline fun <E : Any, K : Any, M : MutableMap<in K?, E>> EntityGrouping<E, *, K>.reduceTo(
    destination: M,
    operation: (key: K?, accumulator: E, element: E) -> E
): M {
    return asKotlinGrouping().reduceTo(destination, operation)
}
