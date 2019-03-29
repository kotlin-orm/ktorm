package me.liuwj.ktorm.entity

import me.liuwj.ktorm.dsl.Query
import me.liuwj.ktorm.dsl.QueryRowSet
import me.liuwj.ktorm.dsl.and
import me.liuwj.ktorm.dsl.not
import me.liuwj.ktorm.expression.ScalarExpression
import me.liuwj.ktorm.expression.SelectExpression
import me.liuwj.ktorm.schema.Table
import java.util.NoSuchElementException

abstract class EntitySequence<T : Table<*>, out R>(val sourceTable: T) {

    abstract fun createQuery(): Query

    abstract fun obtainRow(row: QueryRowSet): R

    operator fun iterator() = object : Iterator<R> {
        private val query = createQuery()
        private var hasNext: Boolean? = null

        override fun hasNext(): Boolean {
            return hasNext ?: query.rowSet.next().also { hasNext = it }
        }

        override fun next(): R {
            return if (hasNext()) obtainRow(query.rowSet).also { hasNext = null } else throw NoSuchElementException()
        }
    }
}

internal fun EntitySequence<*, *>.createSelectExpression() = createQuery().expression as SelectExpression

fun <E : Entity<E>, T : Table<E>> T.asSequence(): EntitySequence<T, E> {
    return object : EntitySequence<T, E>(sourceTable = this) {

        override fun createQuery(): Query {
            return sourceTable.joinReferencesAndSelect()
        }

        override fun obtainRow(row: QueryRowSet): E {
            return sourceTable.createEntity(row)
        }
    }
}

fun <R> EntitySequence<*, R>.toList(): List<R> {
    return this.mapTo(ArrayList()) { it }
}

fun <T : Table<*>, S, R> EntitySequence<T, S>.map(transform: (S) -> R): EntitySequence<T, R> {
    return this.mapIndexed { _, item -> transform(item) }
}

fun <T : Table<*>, S, R> EntitySequence<T, S>.mapIndexed(transform: (index: Int, S) -> R): EntitySequence<T, R> {
    return object : EntitySequence<T, R>(sourceTable) {
        val upstream = this@mapIndexed
        var index = 0

        override fun createQuery(): Query {
            return upstream.createQuery()
        }

        override fun obtainRow(row: QueryRowSet): R {
            return transform(index++, upstream.obtainRow(row))
        }
    }
}

fun <T : Table<*>, S, R, C : MutableCollection<in R>> EntitySequence<T, S>.mapTo(destination: C, transform: (S) -> R): C {
    return this.mapIndexedTo(destination) { _, item -> transform(item) }
}

fun <T : Table<*>, S, R, C : MutableCollection<in R>> EntitySequence<T, S>.mapIndexedTo(destination: C, transform: (index: Int, S) -> R): C {
    var index = 0
    for (item in this) {
        destination.add(transform(index++, item))
    }
    return destination
}

fun <T : Table<*>, R> EntitySequence<T, R>.filter(predicate: (T) -> ScalarExpression<Boolean>): EntitySequence<T, R> {
    return object : EntitySequence<T, R>(sourceTable) {
        val upstream = this@filter

        override fun createQuery(): Query {
            val select = upstream.createSelectExpression()
            if (select.where == null) {
                return Query(select.copy(where = predicate(sourceTable)))
            } else {
                return Query(select.copy(where = select.where and predicate(sourceTable)))
            }
        }

        override fun obtainRow(row: QueryRowSet): R {
            return upstream.obtainRow(row)
        }
    }
}

fun <T : Table<*>, R> EntitySequence<T, R>.filterNot(predicate: (T) -> ScalarExpression<Boolean>): EntitySequence<T, R> {
    return this.filter { !predicate(it) }
}

fun <T : Table<*>, R, C : MutableCollection<in R>> EntitySequence<T, R>.filterTo(destination: C, predicate: (T) -> ScalarExpression<Boolean>): C {
    return this.filter(predicate).mapTo(destination) { it }
}

fun <T : Table<*>, R, C : MutableCollection<in R>> EntitySequence<T, R>.filterNotTo(destination: C, predicate: (T) -> ScalarExpression<Boolean>): C {
    return this.filterNot(predicate).mapTo(destination) { it }
}