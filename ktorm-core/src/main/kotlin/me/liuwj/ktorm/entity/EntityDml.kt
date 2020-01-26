/*
 * Copyright 2018-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.liuwj.ktorm.entity

import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.AliasRemover
import me.liuwj.ktorm.dsl.delete
import me.liuwj.ktorm.dsl.deleteAll
import me.liuwj.ktorm.expression.*
import me.liuwj.ktorm.schema.*

/**
 * Insert the given entity into this table and return the affected record number.
 *
 * If we use an auto-increment key in our table, we need to tell Ktorm which column is the primary key by calling
 * the [Table.primaryKey] function on the column registration, then this function will obtain
 * the generated key from the database and fill it into the corresponding property after the insertion completes.
 * But this requires us not to set the primary key’s value beforehand, otherwise, if you do that, the given value
 * will be inserted into the database, and no keys generated.
 */
@Suppress("DEPRECATION")
@Deprecated(
    message = "This function will be removed in the future. Please use database.sequenceOf(..).add(entity) instead.",
    replaceWith = ReplaceWith("database.sequenceOf(this).add(entity)")
)
fun <E : Entity<E>> Table<E>.add(entity: E): Int {
    return Database.global.sequenceOf(this).add(entity)
}

/**
 * Insert the given entity into this sequence and return the affected record number.
 *
 * If we use an auto-increment key in our table, we need to tell Ktorm which column is the primary key by calling
 * [Table.primaryKey] on the column registration first, then this function will obtain the generated key from the
 * database and fill it into the corresponding property after the insertion completes. But this requires us not to
 * set the primary key’s value beforehand, otherwise, if you do that, the given value will be inserted into the
 * database, and no keys generated.
 *
 * @since 2.7
 */
@Suppress("UNCHECKED_CAST")
fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.add(entity: E): Int {
    checkIfSequenceModified()
    entity.implementation.checkUnexpectedDiscarding(sourceTable)

    val assignments = sourceTable.findInsertColumns(entity).takeIf { it.isNotEmpty() } ?: return 0

    val expression = AliasRemover.visit(
        expr = InsertExpression(
            table = sourceTable.asExpression(),
            assignments = assignments.map { (col, argument) ->
                ColumnAssignmentExpression(
                    column = col.asExpression() as ColumnExpression<Any>,
                    expression = ArgumentExpression(argument, col.sqlType as SqlType<Any>)
                )
            }
        )
    )

    val ignoreGeneratedKeys =
        sourceTable.primaryKey?.binding == null || entity.implementation.getPrimaryKeyValue(sourceTable) != null

    if (ignoreGeneratedKeys) {
        val effects = database.executeUpdate(expression)
        entity.implementation.fromDatabase = database
        entity.implementation.fromTable = sourceTable
        entity.implementation.doDiscardChanges()
        return effects
    } else {
        val (effects, rowSet) = database.executeUpdateAndRetrieveKeys(expression)

        if (rowSet.next()) {
            val generatedKey = sourceTable.primaryKey?.sqlType?.getResult(rowSet, 1)
            if (generatedKey != null) {
                if (database.logger.isDebugEnabled()) {
                    database.logger.debug("Generated Key: $generatedKey")
                }

                entity.implementation.setPrimaryKeyValue(sourceTable, generatedKey)
            }
        }

        entity.implementation.fromDatabase = database
        entity.implementation.fromTable = sourceTable
        entity.implementation.doDiscardChanges()
        return effects
    }
}

private fun Table<*>.findInsertColumns(entity: Entity<*>): Map<Column<*>, Any?> {
    val implementation = entity.implementation
    val assignments = LinkedHashMap<Column<*>, Any?>()

    for (column in columns) {
        if (column.binding != null) {
            val value = implementation.getColumnValue(column.binding)
            if (value != null) {
                assignments[column] = value
            }
        }
    }

    return assignments
}

private fun EntitySequence<*, *>.checkIfSequenceModified() {
    val isModified = expression.where != null
        || expression.groupBy.isNotEmpty()
        || expression.having != null
        || expression.isDistinct
        || expression.orderBy.isNotEmpty()
        || expression.offset != null
        || expression.limit != null

    if (isModified) {
        throw UnsupportedOperationException(
            "Entity manipulation functions are not supported by this sequence object. " +
            "Please call on the origin sequence returned from database.sequenceOf(table)"
        )
    }
}

/**
 * Remove all of the elements of this sequence that satisfy the given [predicate].
 */
fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.removeIf(
    predicate: (T) -> ColumnDeclaring<Boolean>
): Int {
    checkIfSequenceModified()
    return database.delete(sourceTable, predicate)
}

/**
 * Remove all of the elements of this sequence. The sequence will be empty after this function returns.
 */
fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.clear(): Int {
    checkIfSequenceModified()
    return database.deleteAll(sourceTable)
}

@Suppress("UNCHECKED_CAST")
internal fun EntityImplementation.doFlushChanges(): Int {
    check(parent == null) { "The entity is not associated with any database yet." }

    val fromDatabase = fromDatabase ?: error("The entity is not associated with any database yet.")
    val fromTable = fromTable ?: error("The entity is not associated with any table yet.")
    checkUnexpectedDiscarding(fromTable)

    val primaryKey = fromTable.primaryKey ?: error("Table ${fromTable.tableName} doesn't have a primary key.")
    val assignments = findChangedColumns(fromTable).takeIf { it.isNotEmpty() } ?: return 0

    val expression = AliasRemover.visit(
        expr = UpdateExpression(
            table = fromTable.asExpression(),
            assignments = assignments.map { (col, argument) ->
                ColumnAssignmentExpression(
                    column = col.asExpression() as ColumnExpression<Any>,
                    expression = ArgumentExpression(argument, col.sqlType as SqlType<Any>)
                )
            },
            where = BinaryExpression(
                type = BinaryExpressionType.EQUAL,
                left = primaryKey.asExpression(),
                right = ArgumentExpression(getPrimaryKeyValue(fromTable), primaryKey.sqlType as SqlType<Any>),
                sqlType = BooleanSqlType
            )
        )
    )

    return fromDatabase.executeUpdate(expression).also { doDiscardChanges() }
}

private fun EntityImplementation.findChangedColumns(fromTable: Table<*>): Map<Column<*>, Any?> {
    val assignments = LinkedHashMap<Column<*>, Any?>()

    for (column in fromTable.columns) {
        val binding = column.binding ?: continue

        when (binding) {
            is ReferenceBinding -> {
                if (binding.onProperty.name in changedProperties) {
                    val child = this.getProperty(binding.onProperty.name) as Entity<*>?
                    assignments[column] = child?.implementation?.getPrimaryKeyValue(binding.referenceTable as Table<*>)
                }
            }
            is NestedBinding -> {
                var anyChanged = false
                var curr: Any? = this

                for (prop in binding.properties) {
                    if (curr is Entity<*>) {
                        curr = curr.implementation
                    }

                    check(curr is EntityImplementation?)

                    if (curr != null && prop.name in curr.changedProperties) {
                        anyChanged = true
                    }

                    curr = curr?.getProperty(prop.name)
                }

                if (anyChanged) {
                    assignments[column] = curr
                }
            }
        }
    }

    return assignments
}

internal fun EntityImplementation.doDiscardChanges() {
    check(parent == null) { "The entity is not associated with any database yet." }
    val fromTable = fromTable ?: error("The entity is not associated with any table yet.")

    for (column in fromTable.columns) {
        val binding = column.binding ?: continue

        when (binding) {
            is ReferenceBinding -> {
                changedProperties.remove(binding.onProperty.name)
            }
            is NestedBinding -> {
                var curr: Any? = this

                for (prop in binding.properties) {
                    if (curr == null) {
                        break
                    }
                    if (curr is Entity<*>) {
                        curr = curr.implementation
                    }

                    check(curr is EntityImplementation)
                    curr.changedProperties.remove(prop.name)
                    curr = curr.getProperty(prop.name)
                }
            }
        }
    }
}

// Add check to avoid bug #10
private fun EntityImplementation.checkUnexpectedDiscarding(fromTable: Table<*>) {
    for (column in fromTable.columns) {
        if (column.binding !is NestedBinding) continue

        var curr: Any? = this
        for ((i, prop) in column.binding.properties.withIndex()) {
            if (curr == null) {
                break
            }
            if (curr is Entity<*>) {
                curr = curr.implementation
            }

            check(curr is EntityImplementation)

            if (i > 0 && prop.name in curr.changedProperties) {
                val isExternalEntity = curr.fromTable != null && curr.getRoot() != this
                if (isExternalEntity) {
                    val propPath = column.binding.properties.subList(0, i + 1).joinToString(separator = ".") { it.name }
                    val msg = "this.$propPath may be unexpectedly discarded, please save it to database first."
                    throw IllegalStateException(msg)
                }
            }

            curr = curr.getProperty(prop.name)
        }
    }
}

private tailrec fun EntityImplementation.getRoot(): EntityImplementation {
    val parent = this.parent
    if (parent == null) {
        return this
    } else {
        return parent.getRoot()
    }
}

internal fun Entity<*>.clearChangesRecursively() {
    implementation.changedProperties.clear()

    for ((_, value) in properties) {
        if (value is Entity<*>) {
            value.clearChangesRecursively()
        }
    }
}

@Suppress("UNCHECKED_CAST")
internal fun EntityImplementation.doDelete(): Int {
    check(parent == null) { "The entity is not associated with any database yet." }

    val fromDatabase = fromDatabase ?: error("The entity is not associated with any database yet.")
    val fromTable = fromTable ?: error("The entity is not associated with any table yet.")
    val primaryKey = fromTable.primaryKey ?: error("Table ${fromTable.tableName} doesn't have a primary key.")

    val expression = AliasRemover.visit(
        expr = DeleteExpression(
            table = fromTable.asExpression(),
            where = BinaryExpression(
                type = BinaryExpressionType.EQUAL,
                left = primaryKey.asExpression(),
                right = ArgumentExpression(getPrimaryKeyValue(fromTable), primaryKey.sqlType as SqlType<Any>),
                sqlType = BooleanSqlType
            )
        )
    )

    return fromDatabase.executeUpdate(expression)
}
