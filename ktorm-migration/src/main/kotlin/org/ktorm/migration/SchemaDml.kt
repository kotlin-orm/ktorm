package org.ktorm.migration

import org.ktorm.expression.ArgumentExpression
import org.ktorm.expression.ScalarExpression
import org.ktorm.schema.*

public abstract class Constraint<in T>() {}
public data class ForeignKeyConstraint(val to: BaseTable<*>, val on: Column<*>): Constraint<Any>()
public data class DefaultConstraint<T: Any>(val value: ScalarExpression<T>): Constraint<Any>()
public object NotNullConstraint: Constraint<Any>()
public object UniqueConstraint: Constraint<Any>()

public val <C: Any> Column<C>.constraints: Set<Constraint<C>> get() {
    @Suppress("UNCHECKED_CAST")
    return this.extraProperties.getOrDefault("constraints", setOf<Constraint<C>>()) as Set<Constraint<C>>
}
public fun <C: Any> Column<C>.setConstraints(set: Set<Constraint<C>>): Column<C> {
    return this.copy(extraProperties = extraProperties + ("constraints" to set))
}
public fun <C: Any> Column<C>.addConstraint(constraint: Constraint<C>): Column<C> {
    return setConstraints(constraints + constraint)
}
public fun <C: Any> Column<C>.removeConstraint(constraint: Constraint<C>): Column<C> {
    return setConstraints(constraints - constraint)
}

public fun <C : Any> Column<C>.unique(): Column<C> = addConstraint(UniqueConstraint)

public fun <C : Any> Column<C>.foreignKey(
    to: BaseTable<*>,
    @Suppress("UNCHECKED_CAST") on: Column<C> = to.primaryKeys.singleOrNull() as? Column<C> ?: throw IllegalArgumentException("Foreign key cannot be defined this way if there are multiple primary keys on the other")
): Column<C> = addConstraint(ForeignKeyConstraint(to, on))

public fun <C : Any> Column<C>.notNull(): Column<C> {
    return this.addConstraint(NotNullConstraint)
}
public fun <C : Any> Column<C>.default(value: ScalarExpression<C>): Column<C> {
    return this.addConstraint(DefaultConstraint(value))
}
public fun <C : Any> Column<C>.default(value: C): Column<C> {
    return this.addConstraint(DefaultConstraint(ArgumentExpression(value, this.sqlType)))
}