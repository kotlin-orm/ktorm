package me.liuwj.ktorm.entity

import me.liuwj.ktorm.schema.*
import java.util.*
import kotlin.reflect.KClass

internal fun EntityImplementation.getPrimaryKeyValue(fromTable: Table<*>): Any? {
    val primaryKey = fromTable.primaryKey ?: error("Table ${fromTable.tableName} doesn't have a primary key.")
    return getColumnValue(primaryKey)
}

internal fun EntityImplementation.getColumnValue(column: Column<*>): Any? {
    val binding = column.binding ?: error("Column $column has no bindings to any entity field.")

    when (binding) {
        is ReferenceBinding -> {
            val child = this.getProperty(binding.onProperty.name) as Entity<*>?
            return child?.implementation?.getPrimaryKeyValue(binding.referenceTable)
        }
        is NestedBinding -> {
            var curr: EntityImplementation? = this
            for ((i, prop) in binding.withIndex()) {
                if (i != binding.lastIndex) {
                    val child = curr?.getProperty(prop.name) as Entity<*>?
                    curr = child?.implementation
                }
            }
            return curr?.getProperty(binding.last().name)
        }
    }
}

internal fun EntityImplementation.setPrimaryKeyValue(fromTable: Table<*>, value: Any?) {
    val primaryKey = fromTable.primaryKey ?: error("Table ${fromTable.tableName} doesn't have a primary key.")
    setColumnValue(primaryKey, value)
}

internal fun EntityImplementation.setColumnValue(column: Column<*>, value: Any?) {
    val binding = column.binding ?: error("Column $column has no bindings to any entity field.")

    when (binding) {
        is ReferenceBinding -> {
            var child = this.getProperty(binding.onProperty.name) as Entity<*>?
            if (child == null) {
                child = Entity.create(binding.onProperty.returnType.classifier as KClass<*>, fromTable = binding.referenceTable)
                this.setProperty(binding.onProperty.name, child)
            }

            child.implementation.setPrimaryKeyValue(binding.referenceTable, value)
        }
        is NestedBinding -> {
            var curr: EntityImplementation = this
            for ((i, prop) in binding.withIndex()) {
                if (i != binding.lastIndex) {
                    var child = curr.getProperty(prop.name) as Entity<*>?
                    if (child == null) {
                        child = Entity.create(prop.returnType.classifier as KClass<*>, parent = curr, fromTable = column.table)
                        curr.setProperty(prop.name, child)
                    }

                    curr = child.implementation
                }
            }

            curr.setProperty(binding.last().name, value)
        }
    }
}

internal fun EntityImplementation.forceSetPrimaryKeyValue(fromTable: Table<*>, value: Any?) {
    val primaryKey = fromTable.primaryKey ?: error("Table ${fromTable.tableName} doesn't have a primary key.")
    forceSetColumnValue(primaryKey, value)
}

internal fun EntityImplementation.forceSetColumnValue(column: Column<*>, value: Any?) {
    val binding = column.binding ?: error("Column $column has no bindings to any entity field.")

    when (binding) {
        is ReferenceBinding -> {
            var child = this.getProperty(binding.onProperty.name) as Entity<*>?
            if (child == null) {
                child = Entity.create(binding.onProperty.returnType.classifier as KClass<*>, fromTable = binding.referenceTable)
                this.setProperty(binding.onProperty.name, child, forceSet = true)
            }

            child.implementation.forceSetPrimaryKeyValue(binding.referenceTable, value)
        }
        is NestedBinding -> {
            var curr: EntityImplementation = this
            for ((i, prop) in binding.withIndex()) {
                if (i != binding.lastIndex) {
                    var child = curr.getProperty(prop.name) as Entity<*>?
                    if (child == null) {
                        child = Entity.create(prop.returnType.classifier as KClass<*>, parent = curr, fromTable = column.table)
                        curr.setProperty(prop.name, child, forceSet = true)
                    }

                    curr = child.implementation
                }
            }

            curr.setProperty(binding.last().name, value, forceSet = true)
        }
    }
}

internal fun EntityImplementation.isPrimaryKey(name: String): Boolean {
    val binding = this.fromTable?.primaryKey?.binding

    when (binding) {
        null -> return false
        is ReferenceBinding -> {
            return binding.onProperty.name == name
        }
        is NestedBinding -> {
            val namesPath = LinkedList<Set<String>>()
            namesPath.addFirst(setOf(name))

            var curr: EntityImplementation = this
            while (true) {
                val parent = curr.parent ?: break
                val children = parent.values.filterValues { it == curr }

                if (children.isEmpty()) {
                    break
                } else {
                    namesPath.addFirst(children.keys)
                    curr = parent
                }
            }

            for ((i, possibleFields) in namesPath.withIndex()) {
                if (binding[i].name !in possibleFields) {
                    return false
                }
            }

            return true
        }
    }
}

internal val Entity<*>.implementation: EntityImplementation get() {
    return java.lang.reflect.Proxy.getInvocationHandler(this) as EntityImplementation
}