package org.ktorm.dsl

import org.ktorm.expression.ArgumentExpression
import org.ktorm.expression.ScalarExpression
import org.ktorm.schema.*


public fun <C : Any> Column<C>.unique(): Column<C> {
    return this.copy(constraints = constraints + UniqueConstraint)
}
public fun <C : Any> Column<C>.foreignKey(
    to: BaseTable<*>,
    @Suppress("UNCHECKED_CAST") on: Column<C> = to.singlePrimaryKey {
        "Foreign key cannot be defined this way if there are multiple primary keys on the other"
    } as Column<C>
): Column<C> {
    return this.copy(constraints = constraints + ForeignKeyConstraint(to, on))
}
public fun <C : Any> Column<C>.notNull(): Column<C> {
    return this.copy(constraints = constraints + NotNullConstraint)
}
public fun <C : Any> Column<C>.default(value: ScalarExpression<C>): Column<C> {
    return this.copy(constraints = constraints + DefaultConstraint(value))
}
public fun <C : Any> Column<C>.default(value: C): Column<C> {
    return this.copy(constraints = constraints + DefaultConstraint(ArgumentExpression(value, this.sqlType)))
}