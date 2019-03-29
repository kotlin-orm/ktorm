package me.liuwj.ktorm.entity

import me.liuwj.ktorm.dsl.Query
import me.liuwj.ktorm.dsl.QueryRowSet
import java.util.NoSuchElementException

interface EntitySequence<out T> {

    fun createQuery(): Query

    fun obtainRow(row: QueryRowSet): T

    operator fun iterator() = object : Iterator<T> {
        private val rs = createQuery().rowSet
        private var hasNext: Boolean? = null

        override fun hasNext(): Boolean {
            return hasNext ?: rs.next().also { hasNext = it }
        }

        override fun next(): T {
            return if (hasNext()) obtainRow(rs).also { hasNext = null } else throw NoSuchElementException()
        }
    }
}

fun <T> EntitySequence<T>.toList(): List<T> {
    val list = ArrayList<T>()
    for (item in this) {
        list += item
    }
    return list
}

fun <T, R> EntitySequence<T>.map(transform: (T) -> R): EntitySequence<R> {
    return object : EntitySequence<R> {
        val upstream = this@map

        override fun createQuery(): Query {
            return upstream.createQuery()
        }

        override fun obtainRow(row: QueryRowSet): R {
            return transform(upstream.obtainRow(row))
        }
    }
}

fun <T, R> EntitySequence<T>.mapIndexed(transform: (index: Int, T) -> R): EntitySequence<R> {
    return object : EntitySequence<R> {
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

fun <T, R, C : MutableCollection<in R>> EntitySequence<T>.mapTo(destination: C, transform: (T) -> R): C {
    for (item in this) {
        destination.add(transform(item))
    }
    return destination
}

fun <T, R, C : MutableCollection<in R>> EntitySequence<T>.mapIndexedTo(destination: C, transform: (index: Int, T) -> R): C {
    var index = 0
    for (item in this) {
        destination.add(transform(index++, item))
    }
    return destination
}