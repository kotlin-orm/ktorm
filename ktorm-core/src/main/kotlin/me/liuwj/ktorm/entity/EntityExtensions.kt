package me.liuwj.ktorm.entity

import me.liuwj.ktorm.schema.NestedBinding
import me.liuwj.ktorm.schema.ReferenceBinding
import me.liuwj.ktorm.schema.SimpleBinding
import me.liuwj.ktorm.schema.Table
import kotlin.reflect.KClass

internal fun Entity<*>.getPrimaryKeyValue(fromTable: Table<*>): Any? {
    val primaryKey = fromTable.primaryKey ?: kotlin.error("Table ${fromTable.tableName} dosen't have a primary key.")
    val binding = primaryKey.binding ?: kotlin.error("Primary key $primaryKey has no bindings to any entity field.")

    when (binding) {
        is SimpleBinding -> {
            return this[binding.property.name]
        }
        is NestedBinding -> {
            val child = this[binding.property1.name] as Entity<*>?
            return child?.get(binding.property2.name)
        }
        is ReferenceBinding -> {
            val child = this[binding.onProperty.name] as Entity<*>?
            return child?.getPrimaryKeyValue(binding.referenceTable)
        }
    }
}

internal fun Entity<*>.setPrimaryKeyValue(fromTable: Table<*>, value: Any?) {
    val primaryKey = fromTable.primaryKey ?: kotlin.error("Table ${fromTable.tableName} dosen't have a primary key.")
    val binding = primaryKey.binding ?: kotlin.error("Primary key $primaryKey has no bindings to any entity field.")

    when (binding) {
        is SimpleBinding -> {
            this[binding.property.name] = value
        }
        is NestedBinding -> {
            var child = this[binding.property1.name] as Entity<*>?
            if (child == null) {
                child = Entity.create(binding.property1.returnType.classifier as KClass<*>, fromTable, binding.property1.name)
                this[binding.property1.name] = child
            }

            child[binding.property2.name] = value
        }
        is ReferenceBinding -> {
            var child = this[binding.onProperty.name] as Entity<*>?
            if (child == null) {
                child = Entity.create(binding.onProperty.returnType.classifier as KClass<*>, binding.referenceTable, null)
                this[binding.onProperty.name] = child
            }

            child.setPrimaryKeyValue(binding.referenceTable, value)
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