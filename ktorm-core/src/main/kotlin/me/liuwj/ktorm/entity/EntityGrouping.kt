package me.liuwj.ktorm.entity

import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.expression.ColumnDeclaringExpression
import me.liuwj.ktorm.schema.ColumnDeclaring
import me.liuwj.ktorm.schema.Table

data class EntityGrouping<E : Entity<E>, T : Table<E>, K : Any>(
    val sequence: EntitySequence<E, T>,
    val keySelector: (T) -> ColumnDeclaring<K>
) {
    fun asKotlinGrouping() = object : Grouping<E, K?> {
        private val allEntities = LinkedHashMap<E, K?>()

        init {
            val keyColumn = keySelector(sequence.sourceTable)
            val expr = sequence.expression.copy(columns = sequence.expression.columns + keyColumn.asDeclaringExpression())

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

inline fun <E : Entity<E>, T : Table<E>, K : Any, C : Any> EntityGrouping<E, T, K>.aggregate(
    aggregationSelector: (T) -> ColumnDeclaring<C>
): MutableMap<K?, C?> {
    return aggregateTo(LinkedHashMap(), aggregationSelector)
}

inline fun <E : Entity<E>, T : Table<E>, K : Any, C : Any, M> EntityGrouping<E, T, K>.aggregateTo(
    destination: M,
    aggregationSelector: (T) -> ColumnDeclaring<C>
): M where M : MutableMap<in K?, in C?> {
    val keyColumn = keySelector(sequence.sourceTable)
    val aggregation = aggregationSelector(sequence.sourceTable)

    val expr = sequence.expression.copy(
        columns = listOf(keyColumn, aggregation).map { ColumnDeclaringExpression(it.asExpression()) },
        groupBy = listOf(keyColumn.asExpression())
    )

    for (row in Query(expr)) {
        val key = keyColumn.sqlType.getResult(row, 1)
        val value = aggregation.sqlType.getResult(row, 2)
        destination[key] = value
    }

    return destination
}

inline fun <E : Entity<E>, T : Table<E>, K : Any, C1 : Any, C2 : Any> EntityGrouping<E, T, K>.aggregate2(
    aggregationSelector: (T) -> Pair<ColumnDeclaring<C1>, ColumnDeclaring<C2>>
): MutableMap<K?, Pair<C1?, C2?>> {
    return aggregate2To(LinkedHashMap(), aggregationSelector)
}

inline fun <E : Entity<E>, T : Table<E>, K : Any, C1 : Any, C2 : Any, M> EntityGrouping<E, T, K>.aggregate2To(
    destination: M,
    aggregationSelector: (T) -> Pair<ColumnDeclaring<C1>, ColumnDeclaring<C2>>
): M where M : MutableMap<in K?, in Pair<C1?, C2?>> {
    val keyColumn = keySelector(sequence.sourceTable)
    val (c1, c2) = aggregationSelector(sequence.sourceTable)

    val expr = sequence.expression.copy(
        columns = listOf(keyColumn, c1, c2).map { ColumnDeclaringExpression(it.asExpression()) },
        groupBy = listOf(keyColumn.asExpression())
    )

    for (row in Query(expr)) {
        val key = keyColumn.sqlType.getResult(row, 1)
        destination[key] = Pair(c1.sqlType.getResult(row, 2), c2.sqlType.getResult(row, 3))
    }

    return destination
}

inline fun <E : Entity<E>, T : Table<E>, K : Any, C1 : Any, C2 : Any, C3 : Any> EntityGrouping<E, T, K>.aggregate3(
    aggregationSelector: (T) -> Triple<ColumnDeclaring<C1>, ColumnDeclaring<C2>, ColumnDeclaring<C3>>
): MutableMap<K?, Triple<C1?, C2?, C3?>> {
    return aggregate3To(LinkedHashMap(), aggregationSelector)
}

inline fun <E : Entity<E>, T : Table<E>, K : Any, C1 : Any, C2 : Any, C3 : Any, M> EntityGrouping<E, T, K>.aggregate3To(
    destination: M,
    aggregationSelector: (T) -> Triple<ColumnDeclaring<C1>, ColumnDeclaring<C2>, ColumnDeclaring<C3>>
): M where M : MutableMap<in K?, in Triple<C1?, C2?, C3?>> {
    val keyColumn = keySelector(sequence.sourceTable)
    val (c1, c2, c3) = aggregationSelector(sequence.sourceTable)

    val expr = sequence.expression.copy(
        columns = listOf(keyColumn, c1, c2, c3).map { ColumnDeclaringExpression(it.asExpression()) },
        groupBy = listOf(keyColumn.asExpression())
    )

    for (row in Query(expr)) {
        val key = keyColumn.sqlType.getResult(row, 1)
        destination[key] = Triple(c1.sqlType.getResult(row, 2), c2.sqlType.getResult(row, 3), c3.sqlType.getResult(row, 4))
    }

    return destination
}

inline fun <E : Entity<E>, K : Any, R> EntityGrouping<E, *, K>.aggregate(
    operation: (key: K?, accumulator: R?, element: E, first: Boolean) -> R
): Map<K?, R> {
    return asKotlinGrouping().aggregate(operation)
}

inline fun <E : Entity<E>, K : Any, R, M : MutableMap<in K?, R>> EntityGrouping<E, *, K>.aggregateTo(
    destination: M,
    operation: (key: K?, accumulator: R?, element: E, first: Boolean) -> R
): M {
    return asKotlinGrouping().aggregateTo(destination, operation)
}

inline fun <E : Entity<E>, K : Any, R> EntityGrouping<E, *, K>.fold(
    initialValueSelector: (key: K?, element: E) -> R,
    operation: (key: K?, accumulator: R, element: E) -> R
): Map<K?, R> {
    return asKotlinGrouping().fold(initialValueSelector, operation)
}

inline fun <E : Entity<E>, K : Any, R, M : MutableMap<in K?, R>> EntityGrouping<E, *, K>.foldTo(
    destination: M,
    initialValueSelector: (key: K?, element: E) -> R,
    operation: (key: K?, accumulator: R, element: E) -> R
): M {
    return asKotlinGrouping().foldTo(destination, initialValueSelector, operation)
}

inline fun <E : Entity<E>, K : Any, R> EntityGrouping<E, *, K>.fold(
    initialValue: R,
    operation: (accumulator: R, element: E) -> R
): Map<K?, R> {
    return asKotlinGrouping().fold(initialValue, operation)
}

inline fun <E : Entity<E>, K : Any, R, M : MutableMap<in K?, R>> EntityGrouping<E, *, K>.foldTo(
    destination: M,
    initialValue: R,
    operation: (accumulator: R, element: E) -> R
): M {
    return asKotlinGrouping().foldTo(destination, initialValue, operation)
}

inline fun <E : Entity<E>, K : Any> EntityGrouping<E, *, K>.reduce(
    operation: (key: K?, accumulator: E, element: E) -> E
): Map<K?, E> {
    return asKotlinGrouping().reduce(operation)
}

inline fun <E : Entity<E>, K : Any, M : MutableMap<in K?, E>> EntityGrouping<E, *, K>.reduceTo(
    destination: M,
    operation: (key: K?, accumulator: E, element: E) -> E
): M {
    return asKotlinGrouping().reduceTo(destination, operation)
}

fun <E : Entity<E>, T : Table<E>, K : Any> EntityGrouping<E, T, K>.eachCount(): Map<K?, Int> {
    return eachCountTo(LinkedHashMap())
}

@Suppress("RedundantLambdaArrow", "UNCHECKED_CAST")
fun <E : Entity<E>, T : Table<E>, K : Any, M : MutableMap<in K?, Int>> EntityGrouping<E, T, K>.eachCountTo(
    destination: M
): M {
    return aggregateTo(destination as MutableMap<in K?, Int?>) { _ -> count() } as M
}

inline fun <E : Entity<E>, T : Table<E>, K : Any, C : Number> EntityGrouping<E, T, K>.eachSumBy(
    columnSelector: (T) -> ColumnDeclaring<C>
): Map<K?, C?> {
    return eachSumByTo(LinkedHashMap(), columnSelector)
}

inline fun <E : Entity<E>, T : Table<E>, K : Any, C : Number, M : MutableMap<in K?, in C?>> EntityGrouping<E, T, K>.eachSumByTo(
    destination: M,
    columnSelector: (T) -> ColumnDeclaring<C>
): M {
    return aggregateTo(destination) { sum(columnSelector(it)) }
}

inline fun <E : Entity<E>, T : Table<E>, K : Any, C : Number> EntityGrouping<E, T, K>.eachMaxBy(
    columnSelector: (T) -> ColumnDeclaring<C>
): Map<K?, C?> {
    return eachMaxByTo(LinkedHashMap(), columnSelector)
}

inline fun <E : Entity<E>, T : Table<E>, K : Any, C : Number, M : MutableMap<in K?, in C?>> EntityGrouping<E, T, K>.eachMaxByTo(
    destination: M,
    columnSelector: (T) -> ColumnDeclaring<C>
): M {
    return aggregateTo(destination) { max(columnSelector(it)) }
}

inline fun <E : Entity<E>, T : Table<E>, K : Any, C : Number> EntityGrouping<E, T, K>.eachMinBy(
    columnSelector: (T) -> ColumnDeclaring<C>
): Map<K?, C?> {
    return eachMinByTo(LinkedHashMap(), columnSelector)
}

inline fun <E : Entity<E>, T : Table<E>, K : Any, C : Number, M : MutableMap<in K?, in C?>> EntityGrouping<E, T, K>.eachMinByTo(
    destination: M,
    columnSelector: (T) -> ColumnDeclaring<C>
): M {
    return aggregateTo(destination) { min(columnSelector(it)) }
}

inline fun <E : Entity<E>, T : Table<E>, K : Any> EntityGrouping<E, T, K>.eachAverageBy(
    columnSelector: (T) -> ColumnDeclaring<out Number>
): Map<K?, Double?> {
    return eachAverageByTo(LinkedHashMap(), columnSelector)
}

inline fun <E : Entity<E>, T : Table<E>, K : Any, M : MutableMap<in K?, in Double?>> EntityGrouping<E, T, K>.eachAverageByTo(
    destination: M,
    columnSelector: (T) -> ColumnDeclaring<out Number>
): M {
    return aggregateTo(destination) { avg(columnSelector(it)) }
}