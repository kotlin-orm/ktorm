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
            val expr = sequence.expression.copy(
                columns = sequence.expression.columns + keyColumn.asDeclaringExpression()
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

inline fun <E : Entity<E>, T : Table<E>, K, C> EntityGrouping<E, T, K>.aggregateColumns(
    aggregationSelector: (T) -> ColumnDeclaring<C>
): Map<K?, C?> where K : Any, C : Any {
    return aggregateColumnsTo(LinkedHashMap(), aggregationSelector)
}

inline fun <E : Entity<E>, T : Table<E>, K, C, M> EntityGrouping<E, T, K>.aggregateColumnsTo(
    destination: M,
    aggregationSelector: (T) -> ColumnDeclaring<C>
): M where K : Any, C : Any, M : MutableMap<in K?, in C?> {
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

fun <E : Entity<E>, T : Table<E>, K> EntityGrouping<E, T, K>.eachCount(): Map<K?, Int> where K : Any {
    return eachCountTo(LinkedHashMap())
}

@Suppress("UNCHECKED_CAST")
fun <E : Entity<E>, T : Table<E>, K, M> EntityGrouping<E, T, K>.eachCountTo(
    destination: M
): M where K : Any, M : MutableMap<in K?, Int> {
    return aggregateColumnsTo(destination as MutableMap<in K?, Int?>) { count() } as M
}

inline fun <E : Entity<E>, T : Table<E>, K, C> EntityGrouping<E, T, K>.eachSumBy(
    columnSelector: (T) -> ColumnDeclaring<C>
): Map<K?, C?> where K : Any, C : Number {
    return eachSumByTo(LinkedHashMap(), columnSelector)
}

inline fun <E : Entity<E>, T : Table<E>, K, C, M> EntityGrouping<E, T, K>.eachSumByTo(
    destination: M,
    columnSelector: (T) -> ColumnDeclaring<C>
): M where K : Any, C : Number, M : MutableMap<in K?, in C?> {
    return aggregateColumnsTo(destination) { sum(columnSelector(it)) }
}

inline fun <E : Entity<E>, T : Table<E>, K, C> EntityGrouping<E, T, K>.eachMaxBy(
    columnSelector: (T) -> ColumnDeclaring<C>
): Map<K?, C?> where K : Any, C : Number {
    return eachMaxByTo(LinkedHashMap(), columnSelector)
}

inline fun <E : Entity<E>, T : Table<E>, K, C, M> EntityGrouping<E, T, K>.eachMaxByTo(
    destination: M,
    columnSelector: (T) -> ColumnDeclaring<C>
): M where K : Any, C : Number, M : MutableMap<in K?, in C?> {
    return aggregateColumnsTo(destination) { max(columnSelector(it)) }
}

inline fun <E : Entity<E>, T : Table<E>, K, C> EntityGrouping<E, T, K>.eachMinBy(
    columnSelector: (T) -> ColumnDeclaring<C>
): Map<K?, C?> where K : Any, C : Number {
    return eachMinByTo(LinkedHashMap(), columnSelector)
}

inline fun <E : Entity<E>, T : Table<E>, K, C, M> EntityGrouping<E, T, K>.eachMinByTo(
    destination: M,
    columnSelector: (T) -> ColumnDeclaring<C>
): M where K : Any, C : Number, M : MutableMap<in K?, in C?> {
    return aggregateColumnsTo(destination) { min(columnSelector(it)) }
}

inline fun <E : Entity<E>, T : Table<E>, K> EntityGrouping<E, T, K>.eachAverageBy(
    columnSelector: (T) -> ColumnDeclaring<out Number>
): Map<K?, Double?> where K : Any {
    return eachAverageByTo(LinkedHashMap(), columnSelector)
}

inline fun <E : Entity<E>, T : Table<E>, K, M> EntityGrouping<E, T, K>.eachAverageByTo(
    destination: M,
    columnSelector: (T) -> ColumnDeclaring<out Number>
): M where K : Any, M : MutableMap<in K?, in Double?> {
    return aggregateColumnsTo(destination) { avg(columnSelector(it)) }
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
