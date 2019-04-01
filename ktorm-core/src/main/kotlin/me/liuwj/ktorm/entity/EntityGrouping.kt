package me.liuwj.ktorm.entity

import me.liuwj.ktorm.dsl.Query
import me.liuwj.ktorm.schema.ColumnDeclaring
import me.liuwj.ktorm.schema.Table
import java.util.*

data class EntityGrouping<E : Entity<E>, T : Table<E>, K : Any>(
    val sequence: EntitySequence<E, T>,
    val keySelector: (T) -> ColumnDeclaring<K>
) {
    private val allEntities by lazy(LazyThreadSafetyMode.NONE) {
        val keyColumn = keySelector(sequence.sourceTable)
        val expr = sequence.expression.copy(columns = sequence.expression.columns + keyColumn.asDeclaringExpression())

        LinkedHashMap<E, K?>().also {
            for (row in Query(expr)) {
                val entity = sequence.sourceTable.createEntity(row)
                val groupKey = keyColumn.sqlType.getResult(row, expr.columns.size)
                it[entity] = groupKey
            }
        }
    }

    fun sourceIterator(): Iterator<E> {
        return allEntities.keys.iterator()
    }

    fun keyOf(element: E): K {
        return allEntities[element] ?: error("The grouping key is null for element: $element")
    }

    fun keyOf(table: T): ColumnDeclaring<K> {
        return keySelector(table)
    }
}

inline fun <E : Entity<E>, K : Any, R> EntityGrouping<E, *, K>.aggregate(
    operation: (key: K, accumulator: R?, element: E, first: Boolean) -> R
): Map<K, R> {
    return aggregateTo(mutableMapOf(), operation)
}

inline fun <E : Entity<E>, K : Any, R, M : MutableMap<in K, R>> EntityGrouping<E, *, K>.aggregateTo(
    destination: M,
    operation: (key: K, accumulator: R?, element: E, first: Boolean) -> R
): M {
    for (e in this.sourceIterator()) {
        val key = keyOf(e)
        val accumulator = destination[key]
        destination[key] = operation(key, accumulator, e, accumulator == null && !destination.containsKey(key))
    }
    return destination
}

inline fun <E : Entity<E>, K : Any, R> EntityGrouping<E, *, K>.fold(
    initialValueSelector: (key: K, element: E) -> R,
    operation: (key: K, accumulator: R, element: E) -> R
): Map<K, R> {
    @Suppress("UNCHECKED_CAST")
    return aggregate { key, acc, e, first -> operation(key, if (first) initialValueSelector(key, e) else acc as R, e) }
}

inline fun <E : Entity<E>, K : Any, R, M : MutableMap<in K, R>> EntityGrouping<E, *, K>.foldTo(
    destination: M,
    initialValueSelector: (key: K, element: E) -> R,
    operation: (key: K, accumulator: R, element: E) -> R
): M {
    @Suppress("UNCHECKED_CAST")
    return aggregateTo(destination) { key, acc, e, first -> operation(key, if (first) initialValueSelector(key, e) else acc as R, e) }
}

inline fun <E : Entity<E>, K : Any, R> EntityGrouping<E, *, K>.fold(
    initialValue: R,
    operation: (accumulator: R, element: E) -> R
): Map<K, R> {
    @Suppress("UNCHECKED_CAST")
    return aggregate { _, acc, e, first -> operation(if (first) initialValue else acc as R, e) }
}

inline fun <E : Entity<E>, K : Any, R, M : MutableMap<in K, R>> EntityGrouping<E, *, K>.foldTo(
    destination: M,
    initialValue: R,
    operation: (accumulator: R, element: E) -> R
): M {
    @Suppress("UNCHECKED_CAST")
    return aggregateTo(destination) { _, acc, e, first -> operation(if (first) initialValue else acc as R, e) }
}

inline fun <E : Entity<E>, K : Any> EntityGrouping<E, *, K>.reduce(
    operation: (key: K, accumulator: E, element: E) -> E
): Map<K, E> {
    return aggregate { key, acc, e, first -> if (first) e else operation(key, acc as E, e) }
}

inline fun <E : Entity<E>, K : Any, M : MutableMap<in K, E>> EntityGrouping<E, *, K>.reduceTo(
    destination: M,
    operation: (key: K, accumulator: E, element: E) -> E
): M {
    return aggregateTo(destination) { key, acc, e, first -> if (first) e else operation(key, acc as E, e) }
}
