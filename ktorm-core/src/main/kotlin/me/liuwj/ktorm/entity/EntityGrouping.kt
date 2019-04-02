package me.liuwj.ktorm.entity

import me.liuwj.ktorm.dsl.Query
import me.liuwj.ktorm.dsl.sum
import me.liuwj.ktorm.expression.AggregateExpression
import me.liuwj.ktorm.expression.AggregateType
import me.liuwj.ktorm.expression.ColumnDeclaringExpression
import me.liuwj.ktorm.schema.ColumnDeclaring
import me.liuwj.ktorm.schema.IntSqlType
import me.liuwj.ktorm.schema.Table

data class EntityGrouping<E : Entity<E>, T : Table<E>, K : Any>(
    val sequence: EntitySequence<E, T>,
    val keySelector: (T) -> ColumnDeclaring<K>
) {
    fun asKotlinGrouping() = object : Grouping<E, K> {
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

        override fun keyOf(element: E): K {
            return allEntities[element] ?: error("The grouping key is null for element $element")
        }
    }
}

inline fun <E : Entity<E>, K : Any, R> EntityGrouping<E, *, K>.aggregate(
    operation: (key: K, accumulator: R?, element: E, first: Boolean) -> R
): Map<K, R> {
    return asKotlinGrouping().aggregate(operation)
}

inline fun <E : Entity<E>, K : Any, R, M : MutableMap<in K, R>> EntityGrouping<E, *, K>.aggregateTo(
    destination: M,
    operation: (key: K, accumulator: R?, element: E, first: Boolean) -> R
): M {
    return asKotlinGrouping().aggregateTo(destination, operation)
}

inline fun <E : Entity<E>, K : Any, R> EntityGrouping<E, *, K>.fold(
    initialValueSelector: (key: K, element: E) -> R,
    operation: (key: K, accumulator: R, element: E) -> R
): Map<K, R> {
    return asKotlinGrouping().fold(initialValueSelector, operation)
}

inline fun <E : Entity<E>, K : Any, R, M : MutableMap<in K, R>> EntityGrouping<E, *, K>.foldTo(
    destination: M,
    initialValueSelector: (key: K, element: E) -> R,
    operation: (key: K, accumulator: R, element: E) -> R
): M {
    return asKotlinGrouping().foldTo(destination, initialValueSelector, operation)
}

inline fun <E : Entity<E>, K : Any, R> EntityGrouping<E, *, K>.fold(
    initialValue: R,
    operation: (accumulator: R, element: E) -> R
): Map<K, R> {
    return asKotlinGrouping().fold(initialValue, operation)
}

inline fun <E : Entity<E>, K : Any, R, M : MutableMap<in K, R>> EntityGrouping<E, *, K>.foldTo(
    destination: M,
    initialValue: R,
    operation: (accumulator: R, element: E) -> R
): M {
    return asKotlinGrouping().foldTo(destination, initialValue, operation)
}

inline fun <E : Entity<E>, K : Any> EntityGrouping<E, *, K>.reduce(
    operation: (key: K, accumulator: E, element: E) -> E
): Map<K, E> {
    return asKotlinGrouping().reduce(operation)
}

inline fun <E : Entity<E>, K : Any, M : MutableMap<in K, E>> EntityGrouping<E, *, K>.reduceTo(
    destination: M,
    operation: (key: K, accumulator: E, element: E) -> E
): M {
    return asKotlinGrouping().reduceTo(destination, operation)
}

fun <E : Entity<E>, T : Table<E>, K : Any> EntityGrouping<E, T, K>.eachCount(): Map<K, Int> {
    return eachCountTo(LinkedHashMap())
}

fun <E : Entity<E>, T : Table<E>, K : Any, M : MutableMap<in K, Int>> EntityGrouping<E, T, K>.eachCountTo(
    destination: M
): M {
    val keyColumn = keySelector(sequence.sourceTable)
    val countExpr = AggregateExpression(
        type = AggregateType.COUNT,
        argument = null,
        isDistinct = false,
        sqlType = IntSqlType
    )

    val expr = sequence.expression.copy(
        columns = listOf(keyColumn.asExpression(), countExpr).map { ColumnDeclaringExpression(it) },
        groupBy = listOf(keyColumn.asExpression())
    )

    for (row in Query(expr)) {
        val groupKey = keyColumn.sqlType.getResult(row, 1) ?: error("One of the grouping key is null.")
        destination[groupKey] = row.getInt(2)
    }

    return destination
}

inline fun <E : Entity<E>, T : Table<E>, K : Any, C : Number> EntityGrouping<E, T, K>.eachSumOf(
    columnSelector: (T) -> ColumnDeclaring<C>
): Map<K, C> {
    return eachSumOfTo(LinkedHashMap(), columnSelector)
}

inline fun <E : Entity<E>, T : Table<E>, K : Any, C : Number, M : MutableMap<in K, in C>> EntityGrouping<E, T, K>.eachSumOfTo(
    destination: M,
    columnSelector: (T) -> ColumnDeclaring<C>
): M {
    val keyColumn = keySelector(sequence.sourceTable)
    val aggregation = sum(columnSelector(sequence.sourceTable))

    val expr = sequence.expression.copy(
        columns = listOf(keyColumn.asExpression(), aggregation).map { ColumnDeclaringExpression(it) },
        groupBy = listOf(keyColumn.asExpression())
    )

    for (row in Query(expr)) {
        val groupKey = keyColumn.sqlType.getResult(row, 1) ?: error("One of the grouping key is null.")
        destination[groupKey] = aggregation.sqlType.getResult(row, 2) ?: error("One of the aggregation result is null")
    }

    return destination
}