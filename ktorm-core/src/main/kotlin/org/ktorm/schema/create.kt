//TODO: This file needs to be renamed to something else


package org.ktorm.schema

import org.ktorm.expression.*

public fun BaseTable<*>.createTable(): CreateTableExpression {
    val tableConstraints = HashMap<String, TableConstraintExpression>()
    return CreateTableExpression(
        name = this.asReferenceExpression(),
        columns = this.columns.map {
            ColumnDeclarationExpression(
                name = it.name,
                sqlType = it.sqlType,
                constraints = mutableListOf<ColumnConstraintExpression<Any>>().apply {
                    if(primaryKeys.contains(it)) add(PrimaryKeyColumnConstraintExpression)
                    if(it.binding is ReferenceBinding) {
                        tableConstraints["FK_" + it.name] = (ForeignKeyTableConstraintExpression(
                            otherTable = it.binding.referenceTable.asReferenceExpression(),
                            correspondence = mapOf(
                                it.asExpression() to it.binding.referenceTable.singlePrimaryKey {
                                    "Doesn't work on multiple PKs"
                                }.asExpression()
                            )
                        ))
                    }
                    for(extra in it.extraBindings){
                        if(extra is ReferenceBinding) {
                            tableConstraints["FK_" + it.name] = (ForeignKeyTableConstraintExpression(
                                otherTable = extra.referenceTable.asReferenceExpression(),
                                correspondence = mapOf(
                                    it.asExpression() to extra.referenceTable.singlePrimaryKey {
                                        "Doesn't work on multiple PKs"
                                    }.asExpression()
                                )
                            ))
                        }
                    }
                    for(constraint in it.constraints){
                        when(constraint){
                            UniqueConstraint -> add(UniqueColumnConstraintExpression)
                            NotNullConstraint -> add(NotNullColumnConstraintExpression)
                            is DefaultConstraint<*>-> @Suppress("UNCHECKED_CAST") add(
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