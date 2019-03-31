package me.liuwj.ktorm.entity

import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.prepareStatement
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.expression.OrderByExpression
import me.liuwj.ktorm.expression.ScalarExpression
import me.liuwj.ktorm.expression.SelectExpression
import me.liuwj.ktorm.schema.Column
import me.liuwj.ktorm.schema.ColumnDeclaring
import me.liuwj.ktorm.schema.Table
import kotlin.math.min

data class EntitySequence<E : Entity<E>, T : Table<E>>(val sourceTable: T, val expression: SelectExpression) {

    val query = Query(expression)

    val sql get() = query.sql

    val rowSet get() = query.rowSet

    val totalRecords get() = query.totalRecords

    operator fun iterator() = object : Iterator<E> {
        private val queryIterator = query.iterator()

        override fun hasNext(): Boolean {
            return queryIterator.hasNext()
        }

        override fun next(): E {
            return sourceTable.createEntity(queryIterator.next())
        }
    }
}

fun <E : Entity<E>, T : Table<E>> T.asSequence(): EntitySequence<E, T> {
    val query = this.joinReferencesAndSelect()
    return EntitySequence(this, query.expression as SelectExpression)
}

fun <E : Entity<E>> EntitySequence<E, *>.toList(): List<E> {
    val list = ArrayList<E>()
    for (item in this) {
        list += item
    }
    return list
}


fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.filterColumns(selector: (T) -> List<Column<*>>): EntitySequence<E, T> {
    val declarations = selector(sourceTable).map { it.asDeclaringExpression() }
    return this.copy(expression = expression.copy(columns = declarations))
}

fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.filter(predicate: (T) -> ScalarExpression<Boolean>): EntitySequence<E, T> {
    if (expression.where == null) {
        return this.copy(expression = expression.copy(where = predicate(sourceTable)))
    } else {
        return this.copy(expression = expression.copy(where = expression.where and predicate(sourceTable)))
    }
}

fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.filterNot(predicate: (T) -> ScalarExpression<Boolean>): EntitySequence<E, T> {
    return this.filter { !predicate(it) }
}

fun <E : Entity<E>, T : Table<E>, C : MutableCollection<in E>> EntitySequence<E, T>.filterTo(destination: C, predicate: (T) -> ScalarExpression<Boolean>): C {
    val sequence = this.filter(predicate)
    for (item in sequence) {
        destination += item
    }
    return destination
}

fun <E : Entity<E>, T : Table<E>, C : MutableCollection<in E>> EntitySequence<E, T>.filterNotTo(destination: C, predicate: (T) -> ScalarExpression<Boolean>): C {
    val sequence = this.filterNot(predicate)
    for (item in sequence) {
        destination += item
    }
    return destination
}

fun EntitySequence<*, *>.count(): Int {
    val countExpr = expression.toCountExpression(keepPaging = true)

    countExpr.prepareStatement { statement, logger ->
        statement.executeQuery().use { rs ->
            if (rs.next()) {
                return rs.getInt(1).also { logger.debug("Count: {}", it) }
            } else {
                val (sql, _) = Database.global.formatExpression(countExpr, beautifySql = true)
                throw IllegalStateException("No result return for sql: $sql")
            }
        }
    }
}

fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.count(predicate: (T) -> ScalarExpression<Boolean>): Int {
    return this.filter(predicate).count()
}

fun EntitySequence<*, *>.none(): Boolean {
    return this.count() == 0
}

fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.none(predicate: (T) -> ScalarExpression<Boolean>): Boolean {
    return this.count(predicate) == 0
}

fun EntitySequence<*, *>.any(): Boolean {
    return this.count() > 0
}

fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.any(predicate: (T) -> ScalarExpression<Boolean>): Boolean {
    return this.count(predicate) > 0
}

fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.all(predicate: (T) -> ScalarExpression<Boolean>): Boolean {
    return this.none { !predicate(it) }
}

fun <E : Entity<E>, K, V> EntitySequence<E, *>.associate(transform: (E) -> Pair<K, V>): Map<K, V> {
    return this.associateTo(LinkedHashMap(), transform)
}

fun <E : Entity<E>, K> EntitySequence<E, *>.associateBy(keySelector: (E) -> K): Map<K, E> {
    return this.associateByTo(LinkedHashMap(), keySelector)
}

fun <E : Entity<E>, K, V> EntitySequence<E, *>.associateBy(keySelector: (E) -> K, valueTransform: (E) -> V): Map<K, V> {
    return this.associateByTo(LinkedHashMap(), keySelector, valueTransform)
}

fun <K : Entity<K>, V> EntitySequence<K, *>.associateWith(valueTransform: (K) -> V): Map<K, V> {
    return this.associateWithTo(LinkedHashMap(), valueTransform)
}

fun <E : Entity<E>, K, V, M : MutableMap<in K, in V>> EntitySequence<E, *>.associateTo(destination: M, transform: (E) -> Pair<K, V>): M {
    for (item in this) {
        destination += transform(item)
    }
    return destination
}

fun <E : Entity<E>, K, M : MutableMap<in K, in E>> EntitySequence<E, *>.associateByTo(destination: M, keySelector: (E) -> K): M {
    for (item in this) {
        destination.put(keySelector(item), item)
    }
    return destination
}

fun <E : Entity<E>, K, V, M : MutableMap<in K, in V>> EntitySequence<E, *>.associateByTo(destination: M, keySelector: (E) -> K, valueTransform: (E) -> V): M {
    for (item in this) {
        destination.put(keySelector(item), valueTransform(item))
    }
    return destination
}

fun <K : Entity<K>, V, M : MutableMap<in K, in V>> EntitySequence<K, *>.associateWithTo(destination: M, valueTransform: (K) -> V): M {
    for (item in this) {
        destination.put(item, valueTransform(item))
    }
    return destination
}

fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.drop(n: Int): EntitySequence<E, T> {
    if (n == 0) {
        return this
    } else {
        val offset = expression.offset ?: 0
        return this.copy(expression = expression.copy(offset = offset + n))
    }
}

fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.take(n: Int): EntitySequence<E, T> {
    val limit = expression.limit ?: Int.MAX_VALUE
    return this.copy(expression = expression.copy(limit = min(limit, n)))
}

fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.elementAtOrNull(index: Int): E? {
    try {
        val iterator = this.drop(index).take(1).iterator()
        if (iterator.hasNext()) {
            return iterator.next()
        } else {
            return null
        }

    } catch (e: UnsupportedOperationException) {

        val iterator = this.iterator()
        var count = 0
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (index == count++) {
                return item
            }
        }
        return null
    }
}

fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.elementAtOrElse(index: Int, defaultValue: (Int) -> E): E {
    return this.elementAtOrNull(index) ?: defaultValue(index)
}

fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.elementAt(index: Int): E {
    return this.elementAtOrNull(index) ?: throw IndexOutOfBoundsException("Sequence doesn't contain element at index $index.")
}

fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.firstOrNull(): E? {
    return this.elementAtOrNull(0)
}

fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.firstOrNull(predicate: (T) -> ScalarExpression<Boolean>): E? {
    return this.filter(predicate).elementAtOrNull(0)
}

fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.first(): E {
    return this.elementAt(0)
}

fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.first(predicate: (T) -> ScalarExpression<Boolean>): E {
    return this.filter(predicate).elementAt(0)
}

fun <E : Entity<E>> EntitySequence<E, *>.lastOrNull(): E? {
    var last: E? = null
    for (item in this) {
        last = item
    }
    return last
}

fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.lastOrNull(predicate: (T) -> ScalarExpression<Boolean>): E? {
    return this.filter(predicate).lastOrNull()
}

fun <E : Entity<E>> EntitySequence<E, *>.last(): E {
    return lastOrNull() ?: throw NoSuchElementException("Sequence is empty.")
}

fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.last(predicate: (T) -> ScalarExpression<Boolean>): E {
    return this.filter(predicate).last()
}

fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.find(predicate: (T) -> ScalarExpression<Boolean>): E? {
    return this.firstOrNull(predicate)
}

fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.findLast(predicate: (T) -> ScalarExpression<Boolean>): E? {
    return this.lastOrNull(predicate)
}

fun <E : Entity<E>, R> EntitySequence<E, *>.fold(initial: R, operation: (acc: R, E) -> R): R {
    var accumulator = initial
    for (item in this) {
        accumulator = operation(accumulator, item)
    }
    return accumulator
}

fun <E : Entity<E>, R> EntitySequence<E, *>.foldIndexed(initial: R, operation: (index: Int, acc: R, E) -> R): R {
    var index = 0
    return this.fold(initial) { acc, e -> operation(index++, acc, e) }
}

fun <E : Entity<E>> EntitySequence<E, *>.forEach(action: (E) -> Unit) {
    for (item in this) action(item)
}

fun <E : Entity<E>> EntitySequence<E, *>.forEachIndexed(action: (index: Int, E) -> Unit) {
    var index = 0
    for (item in this) action(index++, item)
}

fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.sorted(selector: (T) -> List<OrderByExpression>): EntitySequence<E, T> {
    return this.copy(expression = expression.copy(orderBy = selector(sourceTable)))
}

fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.sortedBy(selector: (T) -> ColumnDeclaring<*>): EntitySequence<E, T> {
    return this.sorted { listOf(selector(it).asc()) }
}

fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.sortedByDescending(selector: (T) -> ColumnDeclaring<*>): EntitySequence<E, T> {
    return this.sorted { listOf(selector(it).desc()) }
}