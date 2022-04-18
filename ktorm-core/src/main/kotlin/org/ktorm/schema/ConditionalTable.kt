package org.ktorm.schema

import org.ktorm.dsl.Query
import org.ktorm.dsl.and
import org.ktorm.dsl.where
import org.ktorm.entity.Entity
import org.ktorm.entity.EntitySequence
import org.ktorm.entity.filter
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
 * object Departments : Table<Department>("t_department") {
 *    val id = int("id").primaryKey().bindTo { it.id }.conditionOn { department, column, value ->
 *      if (value != null) column eq value else column eq 1
 *    }
 *    val name = varchar("name").bindTo { it.name }.conditionNotNullOn { department, column, value ->
 *      column like "%$value%"
 *    }
 *    val location = varchar("location").bindTo { it.location } // No conditions will be generated for this field
 * }
 * ```
 *
 * Then, use [filterBy] or [whereBy] to query by entity objects.
 * ```kotlin
 * val entity = Department {
 *     // ...
 * }
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
    private val columnConditions = mutableMapOf<String, (E, Any?) -> ColumnDeclaring<Boolean>?>()

    /**
     * Provides a query condition for the current field(column) to be used when querying by entity.
     * e.g.
     * ```kotlin
     * object Departments : ConditionalTable<Department>("t_department") {
     *     val id = int("id").primaryKey().bindTo { it.id }.conditionOn { department, column, value ->
     *         if (value != null) column eq value else column eq 1
     *     }
     * }
     * ```
     * @see conditionNotNullOn
     *
     * @param condition the query condition.
     */
    public inline fun <reified C : Any> Column<C>.conditionOn(crossinline condition: (E, Column<C>, C?) -> ColumnDeclaring<Boolean>): Column<C> {
        return saveConditionOn { entity, columnValue ->
            condition(entity, this, columnValue as? C)
        }
    }

    /**
     * Provides a query condition for the current field(column) to be used when querying by entity.
     * e.g.
     * ```kotlin
     * object Departments : ConditionalTable<Department>("t_department") {
     *     val id = int("id").primaryKey().bindTo { it.id }.conditionNotNullOn { department, column, value ->
     *         column eq value
     *     }
     * }
     * ```
     * @see conditionOn
     *
     * @param condition the query condition.
     */
    public inline fun <reified C : Any> Column<C>.conditionNotNullOn(crossinline condition: (E, Column<C>, C) -> ColumnDeclaring<Boolean>): Column<C> {
        return saveConditionOn { entity, columnValue ->
            val value = columnValue as C?
            if (value != null) {
                condition(entity, this, columnValue as C)
            } else {
                null
            }
        }
    }


    @PublishedApi
    internal fun <C : Any> Column<C>.saveConditionOn(condition: (E, Any?) -> ColumnDeclaring<Boolean>?): Column<C> {
        this.name

        // merge with 'and'
        columnConditions.merge(name, condition) { old, curr ->
            { entity, value ->
                val condition1 = old(entity, value)
                val condition2 = curr(entity, value)
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
        val properties = entity.properties
        // TODO
        // columnConditions.entries.fold<Map.Entry<String, (E, Any?) -> ColumnDeclaring<Boolean>?>, ColumnDeclaring<Boolean>?>(
        //     null
        // ) { left, (key, value) ->
        //     val property = properties[key]
        //
        //
        //     TODO()
        // }

        return entity.properties.entries.fold<Map.Entry<String, Any?>, ColumnDeclaring<Boolean>?>(null) { left, (key, value) ->
            val conditionFactory = columnConditions[key] ?: return@fold left
            val condition = conditionFactory(entity, value) ?: return@fold left
            if (left == null) condition else left and condition
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
 *     .forEach {
 *         println(it)
 *     }
 * ```
 *
 * @see ConditionalTable
 */
public fun <E : Entity<E>, T : ConditionalTable<E>> EntitySequence<E, T>.filterBy(conditionEntity: E): EntitySequence<E, T> {
    return sourceTable.asCondition(conditionEntity)
        ?.let { condition -> filter { condition } }
        ?: this

}

/**
 * Conditional filtering is performed by the specified entity class [conditionEntity] based
 * on the conditions defined in each field of [table] of type [ConditionalTable].
 *
 * ```kotlin
 * // by Query
 * database.from(Departments)
 *     .select()
 *     .whereBy(Departments, entity)
 *     // Other operations...
 *     .forEach {
 *         println(it)
 *     }
 * ```
 *
 * @see ConditionalTable
 */
public fun <E : Entity<E>, T : ConditionalTable<E>> Query.whereBy(table: T, conditionEntity: E): Query {
    return table.asCondition(conditionEntity)?.let { this.where(it) } ?: this
}

