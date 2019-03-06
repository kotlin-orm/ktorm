package me.liuwj.ktorm.entity

import me.liuwj.ktorm.schema.*
import kotlin.reflect.KClass

internal fun Entity<*>.getPrimaryKeyValue(fromTable: Table<*>): Any? {
    val primaryKey = fromTable.primaryKey ?: kotlin.error("Table ${fromTable.tableName} dosen't have a primary key.")
    return getColumnValue(primaryKey)
}

internal fun Entity<*>.getColumnValue(column: Column<*>): Any? {
    val binding = column.binding ?: kotlin.error("Column $column has no bindings to any entity field.")

    when (binding) {
        is ReferenceBinding -> {
            val child = this[binding.onProperty.name] as Entity<*>?
            return child?.getPrimaryKeyValue(binding.referenceTable)
        }
        is NestedBinding -> {
            var curr: Entity<*>? = this
            for ((i, prop) in binding.withIndex()) {
                if (i != binding.lastIndex) {
                    curr = curr?.get(prop.name) as Entity<*>?
                }
            }
            return curr?.get(binding.last().name)
        }
    }
}

internal fun Entity<*>.setPrimaryKeyValue(fromTable: Table<*>, value: Any?) {
    val primaryKey = fromTable.primaryKey ?: kotlin.error("Table ${fromTable.tableName} dosen't have a primary key.")
    setColumnValue(primaryKey, value)
}

internal fun Entity<*>.setColumnValue(column: Column<*>, value: Any?) {
    val binding = column.binding ?: kotlin.error("Column $column has no bindings to any entity field.")

    when (binding) {
        is ReferenceBinding -> {
            var child = this[binding.onProperty.name] as Entity<*>?
            if (child == null) {
                child = Entity.create(binding.onProperty.returnType.classifier as KClass<*>, fromTable = binding.referenceTable)
                this[binding.onProperty.name] = child
            }

            child.setPrimaryKeyValue(binding.referenceTable, value)
        }
        is NestedBinding -> {
            var curr: Entity<*> = this
            for ((i, prop) in binding.withIndex()) {
                if (i != binding.lastIndex) {
                    var child = curr[prop.name] as Entity<*>?
                    if (child == null) {
                        child = Entity.create(prop.returnType.classifier as KClass<*>, parent = curr, fromTable = column.table)
                        curr[prop.name] = child
                    }

                    curr = child
                }
            }

            curr[binding.last().name] = value
        }
    }
}

internal fun Entity<*>.forceSetPrimaryKeyValue(fromTable: Table<*>, value: Any?) {
    val primaryKey = fromTable.primaryKey ?: kotlin.error("Table ${fromTable.tableName} dosen't have a primary key.")
    forceSetColumnValue(primaryKey, value)
}

internal fun Entity<*>.forceSetColumnValue(column: Column<*>, value: Any?) {
    val binding = column.binding ?: kotlin.error("Column $column has no bindings to any entity field.")

    when (binding) {
        is ReferenceBinding -> {
            var child = this[binding.onProperty.name] as Entity<*>?
            if (child == null) {
                child = Entity.create(binding.onProperty.returnType.classifier as KClass<*>, fromTable = binding.referenceTable)
                this.impl.forceSetValue(binding.onProperty.name, child)
            }

            child.forceSetPrimaryKeyValue(binding.referenceTable, value)
        }
        is NestedBinding -> {
            var curr: Entity<*> = this
            for ((i, prop) in binding.withIndex()) {
                if (i != binding.lastIndex) {
                    var child = curr[prop.name] as Entity<*>?
                    if (child == null) {
                        child = Entity.create(prop.returnType.classifier as KClass<*>, parent = curr, fromTable = column.table)
                        curr.impl.forceSetValue(prop.name, child)
                    }

                    curr = child
                }
            }

            curr.impl.forceSetValue(binding.last().name, value)
        }
    }
}

/**
 * todo: make this extension internal
 */
val Entity<*>.impl: EntityImpl get() {
    if (this is EntityImpl) {
        return this
    } else {
        return java.lang.reflect.Proxy.getInvocationHandler(this) as EntityImpl
    }
}