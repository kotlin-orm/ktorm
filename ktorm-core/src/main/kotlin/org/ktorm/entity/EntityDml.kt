/*
 * Copyright 2018-2023 the original author or authors.
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

package org.ktorm.entity

import org.ktorm.dsl.*
import org.ktorm.dsl.AliasRemover
import org.ktorm.expression.*
import org.ktorm.schema.*

/**
 * Insert the given entity into this sequence and return the affected record number.
 *
 * If we use an auto-increment key in our table, we need to tell Ktorm which is the primary key by calling
 * [Table.primaryKey] while registering columns, then this function will obtain the generated key from the
 * database and fill it into the corresponding property after the insertion completes. But this requires us
 * not to set the primary key’s value beforehand, otherwise, if you do that, the given value will be inserted
 * into the database, and no keys generated.
 *
 * Note that after calling this function, the [entity] will be ATTACHED to the current database.
 *
 * @see Entity.flushChanges
 * @see Entity.delete
 * @since 2.7
 */
@Suppress("UNCHECKED_CAST")
public fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.add(entity: E): Int {
    checkForDml()
    entity.implementation.checkUnexpectedDiscarding(sourceTable)

    val assignments = entity.findInsertColumns(sourceTable)
    if (assignments.isEmpty()) {
        throw IllegalArgumentException("There are no property values to insert in the entity.")
    }

    val expression = database.dialect.createExpressionVisitor(AliasRemover).visit(
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

    val primaryKeys = sourceTable.primaryKeys

    val ignoreGeneratedKeys = primaryKeys.size != 1
        || primaryKeys[0].binding == null
        || entity.implementation.hasColumnValue(primaryKeys[0].binding!!)

    if (ignoreGeneratedKeys) {
        val effects = database.executeUpdate(expression)
        entity.implementation.fromDatabase = database
        entity.implementation.fromTable = sourceTable
        entity.implementation.doDiscardChanges()
        return effects
    } else {
        val (effects, rowSet) = database.executeUpdateAndRetrieveKeys(expression)

        if (rowSet.next()) {
            val generatedKey = rowSet.getGeneratedKey(primaryKeys[0])
            if (generatedKey != null) {
                if (database.logger.isDebugEnabled()) {
                    database.logger.debug("Generated Key: $generatedKey")
                }

                entity.implementation.setColumnValue(primaryKeys[0].binding!!, generatedKey)
            }
        }

        entity.implementation.fromDatabase = database
        entity.implementation.fromTable = sourceTable
        entity.implementation.doDiscardChanges()
        return effects
    }
}

/**
 * Update properties of the given entity to the database and return the affected record number.
 *
 * Note that after calling this function, the [entity] will be ATTACHED to the current database.
 *
 * @see Entity.flushChanges
 * @see Entity.delete
 * @since 3.1.0
 */
@Suppress("UNCHECKED_CAST")
public fun <E : Entity<E>, T : Table<E>> EntitySequence<E, T>.update(entity: E): Int {
    checkForDml()
    entity.implementation.checkUnexpectedDiscarding(sourceTable)

    val assignments = entity.findUpdateColumns(sourceTable)
    if (assignments.isEmpty()) {
        throw IllegalArgumentException("There are no property values to update in the entity.")
    }

    val expression = database.dialect.createExpressionVisitor(AliasRemover).visit(
        expr = UpdateExpression(
            table = sourceTable.asExpression(),
            assignments = assignments.map { (col, argument) ->
                ColumnAssignmentExpression(
                    column = col.asExpression() as ColumnExpression<Any>,
                    expression = ArgumentExpression(argument, col.sqlType as SqlType<Any>)
                )
            },
            where = entity.implementation.constructIdentityCondition(sourceTable)
        )
    )

    val effects = database.executeUpdate(expression)
    entity.implementation.fromDatabase = database
    entity.implementation.fromTable = sourceTable
    entity.implementation.doDiscardChanges()
    return effects
}

/**
 * Remove all the elements of this sequence that satisfy the given [predicate].
 *
 * @since 2.7
 */
public fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.removeIf(
    predicate: (T) -> ColumnDeclaring<Boolean>
): Int {
    checkForDml()
    return database.delete(sourceTable, predicate)
}

/**
 * Remove all the elements of this sequence. The sequence will be empty after this function returns.
 *
 * @since 2.7
 */
public fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.clear(): Int {
    checkForDml()
    return database.deleteAll(sourceTable)
}

/**
 * Update the property changes of this entity into the database and return the affected record number.
 *
 * This function is the implementation of [Entity.flushChanges].
 */
@Suppress("UNCHECKED_CAST")
internal fun EntityImplementation.doFlushChanges(): Int {
    check(parent == null) { "The entity is not attached to any database yet." }

    val fromDatabase = fromDatabase ?: error("The entity is not attached to any database yet.")
    val fromTable = fromTable ?: error("The entity is not attached to any database yet.")
    checkUnexpectedDiscarding(fromTable)

    val assignments = findChangedColumns(fromTable)
    if (assignments.isEmpty()) {
        // Ignore the flushChanges call.
        return 0
    }

    val expression = fromDatabase.dialect.createExpressionVisitor(AliasRemover).visit(
        expr = UpdateExpression(
            table = fromTable.asExpression(),
            assignments = assignments.map { (col, argument) ->
                ColumnAssignmentExpression(
                    column = col.asExpression() as ColumnExpression<Any>,
                    expression = ArgumentExpression(argument, col.sqlType as SqlType<Any>)
                )
            },
            where = constructIdentityCondition(fromTable)
        )
    )

    return fromDatabase.executeUpdate(expression).also { doDiscardChanges() }
}

/**
 * Delete this entity in the database and return the affected record number.
 *
 * This function is the implementation of [Entity.delete].
 */
internal fun EntityImplementation.doDelete(): Int {
    check(parent == null) { "The entity is not attached to any database yet." }

    val fromDatabase = fromDatabase ?: error("The entity is not attached to any database yet.")
    val fromTable = fromTable ?: error("The entity is not attached to any database yet.")

    val expression = fromDatabase.dialect.createExpressionVisitor(AliasRemover).visit(
        expr = DeleteExpression(
            table = fromTable.asExpression(),
            where = constructIdentityCondition(fromTable)
        )
    )

    return fromDatabase.executeUpdate(expression)
}

/**
 * Check if this sequence can be used for entity manipulations.
 */
private fun EntitySequence<*, *>.checkForDml() {
    val isModified = expression.where != null
        || expression.groupBy.isNotEmpty()
        || expression.having != null
        || expression.isDistinct
        || expression.orderBy.isNotEmpty()
        || expression.offset != null
        || expression.limit != null

    if (isModified) {
        val msg = "" +
            "Entity manipulation functions are not supported by this sequence object. " +
            "Please call on the origin sequence returned from database.sequenceOf(table)"
        throw UnsupportedOperationException(msg)
    }
}

/**
 * Return columns associated with their values for insert.
 */
private fun Entity<*>.findInsertColumns(table: Table<*>): Map<Column<*>, Any?> {
    val assignments = LinkedHashMap<Column<*>, Any?>()

    for (column in table.columns) {
        if (column.binding != null && implementation.hasColumnValue(column.binding)) {
            assignments[column] = implementation.getColumnValue(column.binding)
        }
    }

    return assignments
}

/**
 * Return columns associated with their values for update.
 */
private fun Entity<*>.findUpdateColumns(table: Table<*>): Map<Column<*>, Any?> {
    val assignments = LinkedHashMap<Column<*>, Any?>()

    @Suppress("ConvertArgumentToSet")
    for (column in table.columns - table.primaryKeys) {
        if (column.binding != null && implementation.hasColumnValue(column.binding)) {
            assignments[column] = implementation.getColumnValue(column.binding)
        }
    }

    return assignments
}

/**
 * Return changed columns associated with their values.
 */
private fun EntityImplementation.findChangedColumns(fromTable: Table<*>): Map<Column<*>, Any?> {
    val assignments = LinkedHashMap<Column<*>, Any?>()

    for (column in fromTable.columns) {
        val binding = column.binding ?: continue

        when (binding) {
            is ReferenceBinding -> {
                if (binding.onProperty.name in changedProperties) {
                    val child = this.getProperty(binding.onProperty) as Entity<*>?
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

                    curr = curr?.getProperty(prop)
                }

                if (anyChanged) {
                    assignments[column] = curr
                }
            }
        }
    }

    return assignments
}

/**
 * Clear the tracked property changes of this entity.
 *
 * This function is the implementation of [Entity.discardChanges].
 */
internal fun EntityImplementation.doDiscardChanges() {
    check(parent == null) { "The entity is not attached to any database yet." }
    val fromTable = fromTable ?: error("The entity is not attached to any database yet.")

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
                    curr = curr.getProperty(prop)
                }
            }
        }
    }
}

/**
 * Check to avoid unexpected discarding of changed properties, fix bug #10.
 */
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

            curr = curr.getProperty(prop)
        }
    }
}

/**
 * Return the root parent of this entity.
 */
private tailrec fun EntityImplementation.getRoot(): EntityImplementation {
    val parent = this.parent
    if (parent == null) {
        return this
    } else {
        return parent.getRoot()
    }
}

/**
 * Clear all changes for this entity.
 */
internal fun Entity<*>.clearChangesRecursively() {
    implementation.changedProperties.clear()

    for ((_, value) in properties) {
        if (value is Entity<*>) {
            value.clearChangesRecursively()
        }
    }
}

/**
 * Construct the identity condition `where primaryKey = ?` for the table.
 */
@Suppress("UNCHECKED_CAST")
private fun EntityImplementation.constructIdentityCondition(fromTable: Table<*>): ScalarExpression<Boolean> {
    val primaryKeys = fromTable.primaryKeys
    if (primaryKeys.isEmpty()) {
        error("Table '$fromTable' doesn't have a primary key.")
    }

    val conditions = primaryKeys.map { pk ->
        if (pk.binding == null) {
            error("Primary column $pk has no bindings to any entity field.")
        }

        val pkValue = getColumnValue(pk.binding) ?: error("The value of primary key column $pk is null.")

        BinaryExpression(
            type = BinaryExpressionType.EQUAL,
            left = pk.asExpression(),
            right = ArgumentExpression(pkValue, pk.sqlType as SqlType<Any>),
            sqlType = BooleanSqlType
        )
    }

    return conditions.combineConditions().asExpression()
}
