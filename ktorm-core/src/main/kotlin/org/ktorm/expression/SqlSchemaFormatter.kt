/*
 * Copyright 2018-2021 the original author or authors.
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

package org.ktorm.expression

import org.ktorm.database.Database
import org.ktorm.database.DialectFeatureNotSupportedException

/**
 * Subclass of [SqlExpressionVisitor], visiting SQL expression trees using visitor pattern. After the visit completes,
 * the executable SQL string will be generated in the [sql] property with its execution parameters in [parameters].
 *
 * @property database the current database object used to obtain metadata such as identifier quote string.
 * @property beautifySql mark if we should output beautiful SQL strings with line-wrapping and indentation.
 * @property indentSize the indent size.
 * @property sql return the executable SQL string after the visit completes.
 * @property parameters return the SQL's execution parameters after the visit completes.
 */
@Suppress("VariableNaming")
public abstract class SqlSchemaFormatter(
    database: Database,
    beautifySql: Boolean,
    indentSize: Int
) : SqlFormatter(database, beautifySql, indentSize) {

    override fun visit(expr: SqlExpression): SqlExpression {
        return when(expr){
            PrimaryKeyColumnConstraintExpression -> visitPrimaryKeyColumnConstraint(PrimaryKeyColumnConstraintExpression)
            UniqueColumnConstraintExpression -> visitUniqueColumnConstraint(UniqueColumnConstraintExpression)
            NotNullColumnConstraintExpression -> visitNotNullColumnConstraint(NotNullColumnConstraintExpression)
            is TableReferenceExpression -> visitTableReference(expr)
            is CreateTableExpression -> visitCreateTable(expr)
            is DropTableExpression -> visitDropTable(expr)
            is TruncateTableExpression -> visitTruncateTable(expr)
            is AlterTableAddExpression -> visitAlterTableAdd(expr)
            is AlterTableDropColumnExpression -> visitAlterTableDropColumn(expr)
            is AlterTableModifyColumnExpression -> visitAlterTableModifyColumn(expr)
            is AlterTableSetColumnConstraintExpression<*> -> visitAlterTableSetColumnConstraint(expr)
            is AlterTableDropColumnConstraintExpression -> visitAlterTableDropColumnConstraint(expr)
            is AlterTableAddConstraintExpression -> visitAlterTableAddConstraint(expr)
            is AlterTableDropConstraintExpression -> visitAlterTableDropConstraint(expr)
            is CreateIndexExpression -> visitCreateIndex(expr)
            is DropIndexExpression -> visitDropIndex(expr)
            is CreateViewExpression -> visitCreateView(expr)
            is DropViewExpression -> visitDropView(expr)
            is ColumnDeclarationExpression<*> -> visitColumnDeclaration(expr)
            is ForeignKeyTableConstraintExpression -> visitForeignKeyTableConstraint(expr)
            is CheckTableConstraintExpression -> visitCheckTableConstraint(expr)
            is UniqueTableConstraintExpression -> visitUniqueTableConstraint(expr)
            is PrimaryKeyTableConstraintExpression -> visitPrimaryKeyTableConstraint(expr)
            is DefaultColumnConstraintExpression<*> -> visitDefaultColumnConstraint(expr)
            is AutoIncrementColumnConstraintExpression<*> -> visitAutoIncrementColumnConstraint(expr)
            else -> super.visit(expr)
        }
    }

    protected open fun visitCreateTable(expr: CreateTableExpression): SqlExpression {
        writeKeyword("create table ")
        visitTableReference(expr.name)

        write("(")
        var first = true
        for(col in expr.columns){
            if(first) first = false
            else write(", ")
            visitColumnDeclaration(col)
        }
        for(constraint in expr.constraints.entries){
            writeKeyword(", constraint ")
            write(constraint.key)
            write(" ")
            visit(constraint.value)
        }
        write(") ")

        return expr
    }

    protected open fun visitDropTable(expr: DropTableExpression): SqlExpression {
        writeKeyword("drop table ")
        visitTableReference(expr.table)
        return expr
    }

    protected open fun visitTruncateTable(expr: TruncateTableExpression): SqlExpression {
        writeKeyword("truncate table")
        visitTableReference(expr.table)
        return expr
    }
    protected open fun visitAlterTableAdd(expr: AlterTableAddExpression): SqlExpression = expr
    protected open fun visitAlterTableDropColumn(expr: AlterTableDropColumnExpression): SqlExpression = expr
    protected open fun visitAlterTableModifyColumn(expr: AlterTableModifyColumnExpression): SqlExpression = expr
    protected open fun visitAlterTableSetColumnConstraint(expr: AlterTableSetColumnConstraintExpression<*>): SqlExpression = expr
    protected open fun visitAlterTableDropColumnConstraint(expr: AlterTableDropColumnConstraintExpression): SqlExpression = expr
    protected open fun visitAlterTableAddConstraint(expr: AlterTableAddConstraintExpression): SqlExpression = expr
    protected open fun visitAlterTableDropConstraint(expr: AlterTableDropConstraintExpression): SqlExpression = expr
    protected open fun visitCreateIndex(expr: CreateIndexExpression): SqlExpression = expr
    protected open fun visitDropIndex(expr: DropIndexExpression): SqlExpression = expr
    protected open fun visitCreateView(expr: CreateViewExpression): SqlExpression = expr
    protected open fun visitDropView(expr: DropViewExpression): SqlExpression = expr

    protected open fun visitColumnDeclaration(expr: ColumnDeclarationExpression<*>): SqlExpression {
        write(expr.name)
        write(" ")
        writeKeyword(expr.sqlType.typeName)
        for(constraint in expr.constraints){
            write(" ")
            visit(constraint)
        }
        return expr
    }
    protected open fun visitForeignKeyTableConstraint(expr: ForeignKeyTableConstraintExpression): SqlExpression {
        writeKeyword("FOREIGN KEY (")
        val orderedEntries = expr.correspondence.entries.toList()
        var first = true
        for(col in orderedEntries){
            if(first) first = false
            else write(", ")
            write(col.key.name)
        }
        writeKeyword(") REFERENCES ")
        visitTableReference(expr.otherTable)
        write("(")
        first = true
        for(col in orderedEntries){
            if(first) first = false
            else write(", ")
            write(col.value.name)
        }
        write(")")
        return expr
    }
    protected open fun visitCheckTableConstraint(expr: CheckTableConstraintExpression): SqlExpression = expr
    protected open fun visitUniqueTableConstraint(expr: UniqueTableConstraintExpression): SqlExpression = expr
    protected open fun visitPrimaryKeyTableConstraint(expr: PrimaryKeyTableConstraintExpression): SqlExpression = expr
    protected open fun visitPrimaryKeyColumnConstraint(expr: PrimaryKeyColumnConstraintExpression): SqlExpression {
        writeKeyword("PRIMARY KEY")
        return expr
    }
    protected open fun visitUniqueColumnConstraint(expr: UniqueColumnConstraintExpression): SqlExpression {
        writeKeyword("UNIQUE")
        return expr
    }
    protected open fun visitNotNullColumnConstraint(expr: NotNullColumnConstraintExpression): SqlExpression {
        writeKeyword("NOT NULL")
        return expr
    }
    protected open fun visitDefaultColumnConstraint(expr: DefaultColumnConstraintExpression<*>): SqlExpression {
        writeKeyword("DEFAULT ")
        visitScalar(expr.value)
        return expr
    }
    protected open fun visitAutoIncrementColumnConstraint(expr: AutoIncrementColumnConstraintExpression<*>): SqlExpression = TODO()

    protected open fun visitTableReference(expr: TableReferenceExpression): SqlExpression {
        return visitTable(
            TableExpression(
                name = expr.name,
                tableAlias = null,
                catalog = expr.catalog,
                schema = expr.schema,
                isLeafNode = expr.isLeafNode,
                extraProperties = expr.extraProperties
            )
        )
    }
}
