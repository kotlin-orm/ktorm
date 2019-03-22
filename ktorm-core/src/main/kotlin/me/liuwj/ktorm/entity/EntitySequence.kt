package me.liuwj.ktorm.entity

import me.liuwj.ktorm.dsl.Query

interface EntitySequence<out T> {

    operator fun iterator(): EntitySequenceIterator<T>
}

interface EntitySequenceIterator<out T> : Iterator<T> {

    val query: Query

    override fun hasNext(): Boolean

    override fun next(): T
}
