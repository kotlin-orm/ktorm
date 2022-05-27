package org.ktorm.schema

import org.ktorm.dsl.Query
import org.ktorm.dsl.and
import org.ktorm.dsl.where
import org.ktorm.entity.*
import kotlin.reflect.KClass


/**
 * An extended type of [Table], implemented to allow default condition generation for fields by entity query.
 *
 * Provide default fill condition rules for entity queries via [Column.conditionOn] or [Column.conditionNotNullOn],
 * e.g.
 * ```kotlin
 * interface Department : Entity<Department> {
 *    val id: Int
 *    var name: String
 *    var location: String
 * }
 *
 *
 * object Departments : ConditionalTable<Department>("t_department") {
 *    val id = int("id").primaryKey().bindTo { it.id }.conditionOn { column, value -> // this: Department
 *      if (value != null) column eq value else column eq 1
 *    }
 *    val name = varchar("name").bindTo { it.name }.conditionNotNullOn { column, value -> // this: Department
 *      column like "%$value%"
 *    }
 *    val location = varchar("location").bindTo { it.location } // No conditions will be generated for this field(column)
 * }
 * ```
 *
 * Then, use [filterBy] or [whereBy] to query by entity objects.
 * ```kotlin
 * val entity = Department {
 *     // ...
 * }
 *
 * // by EntitySequence
 * database.departments
 *     .filterBy(entity)
 *     // Other operations...
 *     .forEach {
 *         println(it)
 *     }
 *
 * // by Query
 * database.from(Departments)
 *     .select()
 *     .whereBy(Departments, entity)
 *     // Other operations...
 *     .forEach {
 *         println(it)
 *     }
 *
 * ```
 *
 * @see Table
 * @see filterBy
 * @see whereBy
 *
 * @author ForteScarlet
 */
public open class ConditionalTable<E : Entity<E>>(
    tableName: String,
    alias: String? = null,
    catalog: String? = null,
    schema: String? = null,
    entityClass: KClass<E>? = null
) : Table<E>(tableName, alias, catalog, schema, entityClass) {
    private val columnConditions = mutableMapOf<String, (E, EntityImplementation) -> ColumnDeclaring<Boolean>?>()

    /**
     * Provides a query condition for the current field(column) to be used when querying by entity.
     * e.g.
     * ```kotlin
     * object Departments : ConditionalTable<Department>("t_department") {
     *     val id = int("id").primaryKey().bindTo { it.id }.conditionOn { column, value -> // this: Department
     *         if (value != null) column eq value else column eq 1
     *     }
     * }
     * ```
     * @see conditionNotNullOn
     *
     * @param condition the query condition.
     */
    public inline fun <reified C : Any> Column<C>.conditionOn(crossinline condition: E.(column: Column<C>, value: C?) -> ColumnDeclaring<Boolean>): Column<C> {
        return saveColumnCondition { entity, entityImpl ->
            val value = entityImpl.getColumnValueOrNull(this)
            entity.condition(this, value as C?)
        }
    }


    /**
     * Provides a query condition for the current field(column) to be used when querying by entity.
     * e.g.
     * ```kotlin
     * object Departments : ConditionalTable<Department>("t_department") {
     *     val id = int("id").primaryKey().bindTo { it.id }.conditionNotNullOn { column, value -> // this: Department
     *         column eq value
     *     }
     * }
     * ```
     * @see conditionOn
     *
     * @param condition the query condition.
     */
    public inline fun <reified C : Any> Column<C>.conditionNotNullOn(crossinline condition: E.(column: Column<C>, value: C) -> ColumnDeclaring<Boolean>): Column<C> {
        return saveColumnCondition { entity, entityImpl ->
            val value = entityImpl.getColumnValueOrThrow(this)
            if (value != null) {
                entity.condition(this, value as C)
            } else {
                null
            }
        }
    }


    @PublishedApi
    internal fun EntityImplementation.getColumnValueOrNull(column: Column<*>): Any? {
        return column.binding?.let { b -> getColumnValue(b) }
    }


    @PublishedApi
    internal fun EntityImplementation.getColumnValueOrThrow(column: Column<*>): Any? {
        val binding = column.binding
        if (binding != null) {
            return getColumnValue(binding)
        }

        error("Column $column has no bindings to any entity field.")
    }

    @PublishedApi
    internal fun <C : Any> Column<C>.saveColumnCondition(condition: (E, EntityImplementation) -> ColumnDeclaring<Boolean>?): Column<C> {
        // merge by 'and'
        columnConditions.merge(name, condition) { old, curr ->
            { entity, entityImplementation ->
                val condition1 = old(entity, entityImplementation)
                val condition2 = curr(entity, entityImplementation)
                when {
                    condition1 == null && condition2 == null -> null
                    condition1 == null -> condition2
                    condition2 == null -> condition1
                    else -> condition1 and condition2
                }
            }
        }
        return this
    }


    /**
     * Translate the provided entity classes into query conditions.
     *
     * @param entity entity of this table.
     * @return Query conditions as [ColumnDeclaring]&lt;Boolean%gt;, May be null if no condition is generated.
     */
    public fun asCondition(entity: E): ColumnDeclaring<Boolean>? {
        val entityImplementation = entity.implementation
        return columnConditions.values.fold<(E, EntityImplementation) -> ColumnDeclaring<Boolean>?, ColumnDeclaring<Boolean>?>(
            null
        ) { left, factory ->
            val declaring = factory(entity, entityImplementation)
            if (left == null) {
                declaring
            } else {
                if (declaring == null) left else left and declaring
            }
        }
    }


}


/**
 * Conditional filtering is performed by the specified entity class [conditionEntity] based
 * on the conditions defined by each field in [ConditionalTable].
 *
 * ```kotlin
 * database.departments
 *     .filterBy(entity)
 *     // Other operations...
 *     .forEach { println(it) }
 * ```
 *
 * or
 *
 * ```kotlin
 * database.departments
 *     .filterBy(entity) { table, condition ->
 *         condition and (table.location eq LocationWrapper("GuangZhou"))
 *     }
 *     // Other operations...
 *     .forEach { println(it) }
 * ```
 *
 * If you want to handle the case when the condition may be null, you can refer to [filterByOr].
 *
 * @param andThen When the condition exists, you can operate on it.
 *
 * @see ConditionalTable
 * @see filterByOr
 */
public inline fun <E : Entity<E>, T : ConditionalTable<E>> EntitySequence<E, T>.filterBy(
    conditionEntity: E,
    andThen: ((table: T, condition: ColumnDeclaring<Boolean>) -> ColumnDeclaring<Boolean>) = { _, condition -> condition }
): EntitySequence<E, T> {
    return sourceTable.asCondition(conditionEntity)?.let { condition -> filter { table -> andThen(table, condition) } }
        ?: this

}

/**
 * Conditional filtering is performed by the specified entity class [conditionEntity] based
 * on the conditions defined in each field of [table] of type [ConditionalTable].
 *
 * ```kotlin
 * database.from(Departments)
 *     .select()
 *     .whereBy(Departments, entity)
 *     // Other operations...
 *     .forEach { println(it) }
 * ```
 *
 * or
 *
 * ```kotlin
 * database.from(DepartmentsWithCondition)
 *     .select()
 *     .whereBy(DepartmentsWithCondition, entity) {
 *         it and (Departments.location eq LocationWrapper("GuangZhou"))
 *     }
 *     .forEach { println(it) }
 * ```
 *
 * If you want to handle the case when the condition may be null, you can refer to [whereByOr].
 *
 * @param andThen When the condition exists, you can operate on it.
 * @see ConditionalTable
 * @see whereByOr
 */
public inline fun <E : Entity<E>, T : ConditionalTable<E>> Query.whereBy(
    table: T, conditionEntity: E, andThen: ((condition: ColumnDeclaring<Boolean>) -> ColumnDeclaring<Boolean>) = { it }
): Query {
    return table.asCondition(conditionEntity)?.let { this.where(andThen(it)) } ?: this
}


/**
 * Conditional filtering is performed by the specified entity class [conditionEntity] based
 * on the conditions defined by each field in [ConditionalTable].
 *
 *
 * ```kotlin
 * database.departments
 *     .filterByOr(entity) { table, condition ->
 *         condition?.and(table.location eq LocationWrapper("GuangZhou")) // nullable.
 *     }
 *     // Other operations...
 *     .forEach { println(it) }
 * ```
 *
 * If you only care about the presence of conditions, you can refer to [filterBy].
 *
 * @param andThen When the condition exists, you can operate on it.
 *
 * @see ConditionalTable
 * @see filterBy
 */
public inline fun <E : Entity<E>, T : ConditionalTable<E>> EntitySequence<E, T>.filterByOr(
    conditionEntity: E,
    andThen: ((table: T, condition: ColumnDeclaring<Boolean>?) -> ColumnDeclaring<Boolean>?) = { _, condition -> condition }
): EntitySequence<E, T> {
    return andThen(sourceTable, sourceTable.asCondition(conditionEntity))?.let { condition -> filter { condition } }
        ?: this

}

/**
 * Conditional filtering is performed by the specified entity class [conditionEntity] based
 * on the conditions defined in each field of [table] of type [ConditionalTable].
 *
 * ```kotlin
 * database.from(DepartmentsWithCondition)
 *     .select()
 *     .filterByOr(DepartmentsWithCondition, entity) {
 *          // nullable.
 *         it?.and(DepartmentsWithCondition.location eq LocationWrapper("GuangZhou"))
 *     }
 *     .forEach { println(it) }
 * ```
 * If you only care about the presence of conditions, you can refer to [whereBy].
 *
 * @param andThen When the condition exists, you can operate on it.
 *
 * @see ConditionalTable
 * @see whereBy
 */
public inline fun <E : Entity<E>, T : ConditionalTable<E>> Query.whereByOr(
    table: T,
    conditionEntity: E,
    andThen: ((condition: ColumnDeclaring<Boolean>?) -> ColumnDeclaring<Boolean>?) = { it }
): Query {
    return andThen(table.asCondition(conditionEntity))?.let { this.where(it) } ?: this
}

