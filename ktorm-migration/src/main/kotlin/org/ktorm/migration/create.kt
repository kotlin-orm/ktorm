package org.ktorm.migration

import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import org.ktorm.schema.ReferenceBinding

public fun BaseTable<*>.createTable(): CreateTableExpression {
    val tableConstraints = HashMap<String, TableConstraintExpression>()
    return CreateTableExpression(
        name = this.asReferenceExpression(),
        columns = this.columns.map {
            ColumnDeclarationExpression(
                name = it.name,
                sqlType = it.sqlType,
                constraints = mutableListOf<ColumnConstraintExpression<Any>>().apply {
                    if (primaryKeys.contains(it)) add(PrimaryKeyColumnConstraintExpression)
                    (it.binding as? ReferenceBinding)?.let { binding ->
                        handleReferenceBinding(tableConstraints, it, binding)
                    }
                    for (extra in it.extraBindings) {
                        if (extra is ReferenceBinding) {
                            handleReferenceBinding(tableConstraints, it, extra)
                        }
                    }
                    for (constraint in it.constraints) {
                        when (constraint) {
                            UniqueConstraint -> add(UniqueColumnConstraintExpression)
                            NotNullConstraint -> add(NotNullColumnConstraintExpression)
                            is DefaultConstraint<*> -> @Suppress("UNCHECKED_CAST") add(
                                DefaultColumnConstraintExpression(constraint.value) as ColumnConstraintExpression<Any>
                            )
                            is ForeignKeyConstraint -> {
                                tableConstraints["FK_" + it.name] = (ForeignKeyTableConstraintExpression(
                                    otherTable = constraint.to.asReferenceExpression(),
                                    correspondence = mapOf(
                                        it.asExpression() to constraint.on.asExpression()
                                    )
                                ))
                            }
                        }
                    }
                }
            )
        },
        constraints = tableConstraints
    )
}

private fun handleReferenceBinding(
    tableConstraints: HashMap<String, TableConstraintExpression>,
    it: Column<*>,
    binding: ReferenceBinding
) {
    tableConstraints["FK_" + it.name] = (ForeignKeyTableConstraintExpression(
        otherTable = binding.referenceTable.asReferenceExpression(),
        correspondence = mapOf(
            it.asExpression() to (binding.referenceTable.primaryKeys.singleOrNull()
                ?: throw IllegalArgumentException("Foreign key cannot be defined this way if there are multiple primary keys on the other")).asExpression()
        )
    ))
}