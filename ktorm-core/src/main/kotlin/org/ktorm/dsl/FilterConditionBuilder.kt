package org.ktorm.dsl

import org.ktorm.schema.BaseTable
import org.ktorm.schema.ColumnDeclaring

/**
 * @author kam
 * @since 2021/11/9
 *
 * Create and multiple conditions using the current Builder when multiple possible conditions need to take effect at the same time
 */
public open class FilterConditionBuilder<E : Any, T : BaseTable<E>> {

    /**
     * Store expressions and conditions
     */
    public val predicateOnCondition: MutableMap<(T) -> ColumnDeclaring<Boolean>, Boolean> = mutableMapOf()

    /**
     * Splicing when the current condition is not NULL
     */
    public fun andIfNotNull(
        param: Any?,
        predicate: (T) -> ColumnDeclaring<Boolean>
    ) {
        predicateOnCondition.put(predicate, param != null)
    }

    /**
     * Splice when the current condition holds
     */
    public fun andIf(
        condition: Boolean,
        predicate: (T) -> ColumnDeclaring<Boolean>
    ) {
        predicateOnCondition.put(predicate, condition)
    }

    /**
     * Splicing conditions
     */
    public fun and(predicate: (T) -> ColumnDeclaring<Boolean>) {
        predicateOnCondition.put(predicate, true)
    }
}