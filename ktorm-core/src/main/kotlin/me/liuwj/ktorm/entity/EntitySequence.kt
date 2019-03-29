package me.liuwj.ktorm.entity

import me.liuwj.ktorm.dsl.Query
import me.liuwj.ktorm.dsl.and
import me.liuwj.ktorm.dsl.not
import me.liuwj.ktorm.expression.ScalarExpression
import me.liuwj.ktorm.expression.SelectExpression
import me.liuwj.ktorm.schema.Table

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