package me.liuwj.ktorm.entity

import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.expression.ColumnDeclaringExpression
import me.liuwj.ktorm.expression.OrderByExpression
import me.liuwj.ktorm.expression.SelectExpression
import me.liuwj.ktorm.schema.Column
import me.liuwj.ktorm.schema.ColumnDeclaring
import me.liuwj.ktorm.schema.Table
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.min

data class EntitySequence<E : Entity<E>, T : Table<E>>(
    val sourceTable: T,
    val expression: SelectExpression,
    val entityExtractor: (row: QueryRowSet) -> E
) {
    val query = Query(expression)

    val sql get() = query.sql

    val rowSet get() = query.rowSet

    val totalRecords get() = query.totalRecords

    fun asKotlinSequence() = Sequence { iterator() }

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

fun <E : Entity<E>, T : Table<E>> T.asSequence(): EntitySequence<E, T> {
    val query = this.joinReferencesAndSelect()
    return EntitySequence(this, query.expression as SelectExpression) { row -> this.createEntity(row) }
}

fun <E : Entity<E>, T : Table<E>> T.asSequenceWithoutReferences(): EntitySequence<E, T> {
    val query = this.select(columns)
    return EntitySequence(this, query.expression as SelectExpression) { row -> this.createEntityWithoutReferences(row) }
}

fun <E : Entity<E>, C : MutableCollection<in E>> EntitySequence<E, *>.toCollection(destination: C): C {
    return asKotlinSequence().toCollection(destination)
}

fun <E : Entity<E>> EntitySequence<E, *>.toList(): List<E> {
    return asKotlinSequence().toList()
}

fun <E : Entity<E>> EntitySequence<E, *>.toMutableList(): MutableList<E> {
    return asKotlinSequence().toMutableList()
}

fun <E : Entity<E>> EntitySequence<E, *>.toSet(): Set<E> {
    return asKotlinSequence().toSet()
}

fun <E : Entity<E>> EntitySequence<E, *>.toMutableSet(): MutableSet<E> {
    return asKotlinSequence().toMutableSet()
}

fun <E : Entity<E>> EntitySequence<E, *>.toHashSet(): HashSet<E> {
    return asKotlinSequence().toHashSet()
}

fun <E> EntitySequence<E, *>.toSortedSet(): SortedSet<E> where E : Entity<E>, E : Comparable<E> {
    return asKotlinSequence().toSortedSet()
}

fun <E> EntitySequence<E, *>.toSortedSet(
    comparator: Comparator<in E>
): SortedSet<E> where E : Entity<E>, E : Comparable<E> {
    return asKotlinSequence().toSortedSet(comparator)
}

inline fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.filterColumns(
    selector: (T) -> List<Column<*>>
): EntitySequence<E, T> {
    val columns = selector(sourceTable)
    if (columns.isEmpty()) {
        return this
    } else {
        return this.copy(expression = expression.copy(columns = columns.map { it.asDeclaringExpression() }))
    }
}

inline fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.filter(
    predicate: (T) -> ColumnDeclaring<Boolean>
): EntitySequence<E, T> {
    if (expression.where == null) {
        return this.copy(expression = expression.copy(where = predicate(sourceTable).asExpression()))
    } else {
        return this.copy(expression = expression.copy(where = expression.where and predicate(sourceTable)))
    }
}

inline fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.filterNot(
    predicate: (T) -> ColumnDeclaring<Boolean>
): EntitySequence<E, T> {
    return filter { !predicate(it) }
}

inline fun <E : Entity<E>, T : Table<E>, C : MutableCollection<in E>> EntitySequence<E, T>.filterTo(
    destination: C,
    predicate: (T) -> ColumnDeclaring<Boolean>
): C {
    return filter(predicate).toCollection(destination)
}

inline fun <E : Entity<E>, T : Table<E>, C : MutableCollection<in E>> EntitySequence<E, T>.filterNotTo(
    destination: C,
    predicate: (T) -> ColumnDeclaring<Boolean>
): C {
    return filterNot(predicate).toCollection(destination)
}

inline fun <E : Entity<E>, R> EntitySequence<E, *>.map(transform: (E) -> R): List<R> {
    return mapTo(ArrayList(), transform)
}

inline fun <E : Entity<E>, R, C : MutableCollection<in R>> EntitySequence<E, *>.mapTo(
    destination: C,
    transform: (E) -> R
): C {
    for (item in this) destination += transform(item)
    return destination
}

inline fun <E : Entity<E>, R> EntitySequence<E, *>.mapIndexed(transform: (index: Int, E) -> R): List<R> {
    return mapIndexedTo(ArrayList(), transform)
}

inline fun <E : Entity<E>, R, C : MutableCollection<in R>> EntitySequence<E, *>.mapIndexedTo(
    destination: C,
    transform: (index: Int, E) -> R
): C {
    var index = 0
    return mapTo(destination) { transform(index++, it) }
}

inline fun <E : Entity<E>, T : Table<E>, C : Any> EntitySequence<E, T>.mapColumns(
    isDistinct: Boolean = false,
    columnSelector: (T) -> ColumnDeclaring<C>
): List<C?> {
    return mapColumnsTo(ArrayList(), isDistinct, columnSelector)
}

inline fun <E : Entity<E>, T : Table<E>, C : Any, R : MutableCollection<in C?>> EntitySequence<E, T>.mapColumnsTo(
    destination: R,
    isDistinct: Boolean = false,
    columnSelector: (T) -> ColumnDeclaring<C>
): R {
    val column = columnSelector(sourceTable)

    val expr = expression.copy(
        columns = listOf(ColumnDeclaringExpression(column.asExpression())),
        isDistinct = isDistinct
    )

    return Query(expr).mapTo(destination) { row -> column.sqlType.getResult(row, 1) }
}

inline fun <E : Entity<E>, T : Table<E>, C : Any> EntitySequence<E, T>.mapColumnsNotNull(
    isDistinct: Boolean = false,
    columnSelector: (T) -> ColumnDeclaring<C>
): List<C> {
    return mapColumnsNotNullTo(ArrayList(), isDistinct, columnSelector)
}

inline fun <E : Entity<E>, T : Table<E>, C : Any, R : MutableCollection<in C>> EntitySequence<E, T>.mapColumnsNotNullTo(
    destination: R,
    isDistinct: Boolean = false,
    columnSelector: (T) -> ColumnDeclaring<C>
): R {
    val column = columnSelector(sourceTable)

    val expr = expression.copy(
        columns = listOf(ColumnDeclaringExpression(column.asExpression())),
        isDistinct = isDistinct
    )

    return Query(expr).mapNotNullTo(destination) { row -> column.sqlType.getResult(row, 1) }
}

inline fun <E : Entity<E>, T : Table<E>, C1 : Any, C2 : Any> EntitySequence<E, T>.mapColumns2(
    isDistinct: Boolean = false,
    columnSelector: (T) -> Pair<ColumnDeclaring<C1>, ColumnDeclaring<C2>>
): List<Pair<C1?, C2?>> {
    return mapColumns2To(ArrayList(), isDistinct, columnSelector)
}

inline fun <E : Entity<E>, T : Table<E>, C1 : Any, C2 : Any, R> EntitySequence<E, T>.mapColumns2To(
    destination: R,
    isDistinct: Boolean = false,
    columnSelector: (T) -> Pair<ColumnDeclaring<C1>, ColumnDeclaring<C2>>
): R where R : MutableCollection<in Pair<C1?, C2?>> {
    val (c1, c2) = columnSelector(sourceTable)

    val expr = expression.copy(
        columns = listOf(c1, c2).map { ColumnDeclaringExpression(it.asExpression()) },
        isDistinct = isDistinct
    )

    return Query(expr).mapTo(destination) { row -> Pair(c1.sqlType.getResult(row, 1), c2.sqlType.getResult(row, 2)) }
}

inline fun <E : Entity<E>, T : Table<E>, C1 : Any, C2 : Any, C3 : Any> EntitySequence<E, T>.mapColumns3(
    isDistinct: Boolean = false,
    columnSelector: (T) -> Triple<ColumnDeclaring<C1>, ColumnDeclaring<C2>, ColumnDeclaring<C3>>
): List<Triple<C1?, C2?, C3?>> {
    return mapColumns3To(ArrayList(), isDistinct, columnSelector)
}

inline fun <E : Entity<E>, T : Table<E>, C1 : Any, C2 : Any, C3 : Any, R> EntitySequence<E, T>.mapColumns3To(
    destination: R,
    isDistinct: Boolean = false,
    columnSelector: (T) -> Triple<ColumnDeclaring<C1>, ColumnDeclaring<C2>, ColumnDeclaring<C3>>
): R where R : MutableCollection<in Triple<C1?, C2?, C3?>> {
    val (c1, c2, c3) = columnSelector(sourceTable)

    val expr = expression.copy(
        columns = listOf(c1, c2, c3).map { ColumnDeclaringExpression(it.asExpression()) },
        isDistinct = isDistinct
    )

    return Query(expr).mapTo(destination) { row ->
        Triple(c1.sqlType.getResult(row, 1), c2.sqlType.getResult(row, 2), c3.sqlType.getResult(row, 3))
    }
}

inline fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.sorted(
    selector: (T) -> List<OrderByExpression>
): EntitySequence<E, T> {
    return this.copy(expression = expression.copy(orderBy = selector(sourceTable)))
}

inline fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.sortedBy(
    selector: (T) -> ColumnDeclaring<*>
): EntitySequence<E, T> {
    return sorted { listOf(selector(it).asc()) }
}

inline fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.sortedByDescending(
    selector: (T) -> ColumnDeclaring<*>
): EntitySequence<E, T> {
    return sorted { listOf(selector(it).desc()) }
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

inline fun <E : Entity<E>, T : Table<E>, C : Any> EntitySequence<E, T>.aggregate(
    aggregationSelector: (T) -> ColumnDeclaring<C>
): C? {
    val aggregation = aggregationSelector(sourceTable)

    val expr = expression.copy(
        columns = listOf(ColumnDeclaringExpression(aggregation.asExpression()))
    )

    val rowSet = Query(expr).rowSet

    if (rowSet.size() == 1) {
        check(rowSet.next())
        return aggregation.sqlType.getResult(rowSet, 1)
    } else {
        val (sql, _) = Database.global.formatExpression(expr, beautifySql = true)
        throw IllegalStateException("Expected 1 row but ${rowSet.size()} returned from sql: \n\n$sql")
    }
}

inline fun <E : Entity<E>, T : Table<E>, C1 : Any, C2 : Any> EntitySequence<E, T>.aggregate2(
    aggregationSelector: (T) -> Pair<ColumnDeclaring<C1>, ColumnDeclaring<C2>>
): Pair<C1?, C2?> {
    val (c1, c2) = aggregationSelector(sourceTable)

    val expr = expression.copy(
        columns = listOf(c1, c2).map { ColumnDeclaringExpression(it.asExpression()) }
    )

    val rowSet = Query(expr).rowSet

    if (rowSet.size() == 1) {
        check(rowSet.next())
        return Pair(c1.sqlType.getResult(rowSet, 1), c2.sqlType.getResult(rowSet, 2))
    } else {
        val (sql, _) = Database.global.formatExpression(expr, beautifySql = true)
        throw IllegalStateException("Expected 1 row but ${rowSet.size()} returned from sql: \n\n$sql")
    }
}

inline fun <E : Entity<E>, T : Table<E>, C1 : Any, C2 : Any, C3 : Any> EntitySequence<E, T>.aggregate3(
    aggregationSelector: (T) -> Triple<ColumnDeclaring<C1>, ColumnDeclaring<C2>, ColumnDeclaring<C3>>
): Triple<C1?, C2?, C3?> {
    val (c1, c2, c3) = aggregationSelector(sourceTable)

    val expr = expression.copy(
        columns = listOf(c1, c2, c3).map { ColumnDeclaringExpression(it.asExpression()) }
    )

    val rowSet = Query(expr).rowSet

    if (rowSet.size() == 1) {
        check(rowSet.next())
        return Triple(c1.sqlType.getResult(rowSet, 1), c2.sqlType.getResult(rowSet, 2), c3.sqlType.getResult(rowSet, 3))
    } else {
        val (sql, _) = Database.global.formatExpression(expr, beautifySql = true)
        throw IllegalStateException("Expected 1 row but ${rowSet.size()} returned from sql: \n\n$sql")
    }
}

fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.count(): Int {
    return aggregate { me.liuwj.ktorm.dsl.count() } ?: error("Count expression returns null, which never happens.")
}

inline fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.count(
    predicate: (T) -> ColumnDeclaring<Boolean>
): Int {
    return filter(predicate).count()
}

fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.none(): Boolean {
    return count() == 0
}

inline fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.none(
    predicate: (T) -> ColumnDeclaring<Boolean>
): Boolean {
    return count(predicate) == 0
}

fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.any(): Boolean {
    return count() > 0
}

inline fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.any(
    predicate: (T) -> ColumnDeclaring<Boolean>
): Boolean {
    return count(predicate) > 0
}

inline fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.all(
    predicate: (T) -> ColumnDeclaring<Boolean>
): Boolean {
    return none { !predicate(it) }
}

inline fun <E : Entity<E>, T : Table<E>, C : Number> EntitySequence<E, T>.sumBy(
    selector: (T) -> ColumnDeclaring<C>
): C? {
    return aggregate { sum(selector(it)) }
}

inline fun <E : Entity<E>, T : Table<E>, C : Number> EntitySequence<E, T>.maxBy(
    selector: (T) -> ColumnDeclaring<C>
): C? {
    return aggregate { max(selector(it)) }
}

inline fun <E : Entity<E>, T : Table<E>, C : Number> EntitySequence<E, T>.minBy(
    selector: (T) -> ColumnDeclaring<C>
): C? {
    return aggregate { min(selector(it)) }
}

inline fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.averageBy(
    selector: (T) -> ColumnDeclaring<out Number>
): Double? {
    return aggregate { avg(selector(it)) }
}

inline fun <E : Entity<E>, K, V> EntitySequence<E, *>.associate(
    transform: (E) -> Pair<K, V>
): Map<K, V> {
    return asKotlinSequence().associate(transform)
}

inline fun <E : Entity<E>, K> EntitySequence<E, *>.associateBy(
    keySelector: (E) -> K
): Map<K, E> {
    return asKotlinSequence().associateBy(keySelector)
}

inline fun <E : Entity<E>, K, V> EntitySequence<E, *>.associateBy(
    keySelector: (E) -> K,
    valueTransform: (E) -> V
): Map<K, V> {
    return asKotlinSequence().associateBy(keySelector, valueTransform)
}

inline fun <K : Entity<K>, V> EntitySequence<K, *>.associateWith(
    valueTransform: (K) -> V
): Map<K, V> {
    return asKotlinSequence().associateWith(valueTransform)
}

inline fun <E : Entity<E>, K, V, M : MutableMap<in K, in V>> EntitySequence<E, *>.associateTo(
    destination: M,
    transform: (E) -> Pair<K, V>
): M {
    return asKotlinSequence().associateTo(destination, transform)
}

inline fun <E : Entity<E>, K, M : MutableMap<in K, in E>> EntitySequence<E, *>.associateByTo(
    destination: M,
    keySelector: (E) -> K
): M {
    return asKotlinSequence().associateByTo(destination, keySelector)
}

inline fun <E : Entity<E>, K, V, M : MutableMap<in K, in V>> EntitySequence<E, *>.associateByTo(
    destination: M,
    keySelector: (E) -> K,
    valueTransform: (E) -> V
): M {
    return asKotlinSequence().associateByTo(destination, keySelector, valueTransform)
}

inline fun <K : Entity<K>, V, M : MutableMap<in K, in V>> EntitySequence<K, *>.associateWithTo(
    destination: M,
    valueTransform: (K) -> V
): M {
    return asKotlinSequence().associateWithTo(destination, valueTransform)
}

fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.elementAtOrNull(index: Int): E? {
    try {
        return drop(index).take(1).asKotlinSequence().firstOrNull()
    } catch (e: UnsupportedOperationException) {
        return asKotlinSequence().elementAtOrNull(index)
    }
}

inline fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.elementAtOrElse(index: Int, defaultValue: (Int) -> E): E {
    return elementAtOrNull(index) ?: defaultValue(index)
}

fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.elementAt(index: Int): E {
    return elementAtOrNull(index) ?: throw IndexOutOfBoundsException("Sequence doesn't contain element at index $index.")
}

fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.firstOrNull(): E? {
    return elementAtOrNull(0)
}

inline fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.firstOrNull(predicate: (T) -> ColumnDeclaring<Boolean>): E? {
    return filter(predicate).elementAtOrNull(0)
}

fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.first(): E {
    return elementAt(0)
}

inline fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.first(predicate: (T) -> ColumnDeclaring<Boolean>): E {
    return filter(predicate).elementAt(0)
}

fun <E : Entity<E>> EntitySequence<E, *>.lastOrNull(): E? {
    return asKotlinSequence().lastOrNull()
}

inline fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.lastOrNull(predicate: (T) -> ColumnDeclaring<Boolean>): E? {
    return filter(predicate).lastOrNull()
}

fun <E : Entity<E>> EntitySequence<E, *>.last(): E {
    return lastOrNull() ?: throw NoSuchElementException("Sequence is empty.")
}

inline fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.last(predicate: (T) -> ColumnDeclaring<Boolean>): E {
    return filter(predicate).last()
}

inline fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.find(predicate: (T) -> ColumnDeclaring<Boolean>): E? {
    return firstOrNull(predicate)
}

inline fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.findLast(predicate: (T) -> ColumnDeclaring<Boolean>): E? {
    return lastOrNull(predicate)
}

fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.singleOrNull(): E? {
    return asKotlinSequence().singleOrNull()
}

inline fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.singleOrNull(predicate: (T) -> ColumnDeclaring<Boolean>): E? {
    return filter(predicate).singleOrNull()
}

fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.single(): E {
    return asKotlinSequence().single()
}

inline fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.single(predicate: (T) -> ColumnDeclaring<Boolean>): E {
    return filter(predicate).single()
}

inline fun <E : Entity<E>, R> EntitySequence<E, *>.fold(initial: R, operation: (acc: R, E) -> R): R {
    return asKotlinSequence().fold(initial, operation)
}

inline fun <E : Entity<E>, R> EntitySequence<E, *>.foldIndexed(initial: R, operation: (index: Int, acc: R, E) -> R): R {
    return asKotlinSequence().foldIndexed(initial, operation)
}

inline fun <E : Entity<E>> EntitySequence<E, *>.reduce(operation: (acc: E, E) -> E): E {
    return asKotlinSequence().reduce(operation)
}

inline fun <E : Entity<E>> EntitySequence<E, *>.reduceIndexed(operation: (index: Int, acc: E, E) -> E): E {
    return asKotlinSequence().reduceIndexed(operation)
}

inline fun <E : Entity<E>> EntitySequence<E, *>.forEach(action: (E) -> Unit) {
    for (item in this) action(item)
}

inline fun <E : Entity<E>> EntitySequence<E, *>.forEachIndexed(action: (index: Int, E) -> Unit) {
    var index = 0
    for (item in this) action(index++, item)
}

inline fun <E : Entity<E>, K> EntitySequence<E, *>.groupBy(
    keySelector: (E) -> K
): Map<K, List<E>> {
    return asKotlinSequence().groupBy(keySelector)
}

inline fun <E : Entity<E>, K, V> EntitySequence<E, *>.groupBy(
    keySelector: (E) -> K,
    valueTransform: (E) -> V
): Map<K, List<V>> {
    return asKotlinSequence().groupBy(keySelector, valueTransform)
}

inline fun <E : Entity<E>, K, M : MutableMap<in K, MutableList<E>>> EntitySequence<E, *>.groupByTo(
    destination: M,
    keySelector: (E) -> K
): M {
    return asKotlinSequence().groupByTo(destination, keySelector)
}

inline fun <E : Entity<E>, K, V, M : MutableMap<in K, MutableList<V>>> EntitySequence<E, *>.groupByTo(
    destination: M,
    keySelector: (E) -> K,
    valueTransform: (E) -> V
): M {
    return asKotlinSequence().groupByTo(destination, keySelector, valueTransform)
}

fun <E : Entity<E>, T : Table<E>, K : Any> EntitySequence<E, T>.groupingBy(
    keySelector: (T) -> ColumnDeclaring<K>
): EntityGrouping<E, T, K> {
    return EntityGrouping(this, keySelector)
}

fun <E : Entity<E>, A : Appendable> EntitySequence<E, *>.joinTo(
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

fun <E : Entity<E>> EntitySequence<E, *>.joinToString(
    separator: CharSequence = ", ",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    limit: Int = -1,
    truncated: CharSequence = "...",
    transform: ((E) -> CharSequence)? = null
): String {
    return asKotlinSequence().joinToString(separator, prefix, postfix, limit, truncated, transform)
}