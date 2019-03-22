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