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

package org.ktorm.ksp.compiler.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toClassName
import org.ktorm.dsl.AliasRemover
import org.ktorm.entity.EntitySequence
import org.ktorm.expression.ColumnAssignmentExpression
import org.ktorm.expression.UpdateExpression
import org.ktorm.ksp.compiler.util.*
import org.ktorm.ksp.spi.TableMetadata

@OptIn(KotlinPoetKspPreview::class)
internal object UpdateFunctionGenerator {

    fun generate(table: TableMetadata): FunSpec {
        val kdoc = "" +
            "Update the given entity to the database and return the affected record number. " +
            "If [isDynamic] is set to true, the generated SQL will include only the non-null columns. "

        val entityClass = table.entityClass.toClassName()
        val tableClass = ClassName(table.entityClass.packageName.asString(), table.tableClassName)

        return FunSpec.builder("update")
            .addKdoc(kdoc)
            .receiver(EntitySequence::class.asClassName().parameterizedBy(entityClass, tableClass))
            .addParameter("entity", entityClass)
            .addParameter(ParameterSpec.builder("isDynamic", typeNameOf<Boolean>()).defaultValue("false").build())
            .returns(Int::class.asClassName())
            .addCode(AddFunctionGenerator.checkForDml())
            .addCode(AddFunctionGenerator.addValFun())
            .addCode(addAssignments(table))
            .addCode(buildConditions(table))
            .addCode(createExpression())
            .addStatement("return database.executeUpdate(expression)")
            .build()
    }

    private fun addAssignments(table: TableMetadata): CodeBlock {
        return buildCodeBlock {
            addStatement("val assignments = ArrayList<%T<*>>()", ColumnAssignmentExpression::class.asClassName())

            for (column in table.columns) {
                if (column.isPrimaryKey) {
                    continue
                }

                addStatement(
                    "assignments.addVal(sourceTable.%N, entity.%N, isDynamic)",
                    column.columnPropertyName,
                    column.entityProperty.simpleName.asString(),
                )
            }

            add("\n")

            beginControlFlow("if (assignments.isEmpty())")
            addStatement("return 0")
            endControlFlow()

            add("\n")
        }
    }

    private fun buildConditions(table: TableMetadata): CodeBlock {
        return buildCodeBlock {
            add("«val conditions = listOf(")

            for (column in table.columns) {
                if (!column.isPrimaryKey) {
                    continue
                }

                val condition: String
                if (column.entityProperty.type.resolve().isMarkedNullable) {
                    condition = "sourceTable.%N·%M·entity.%N!!,"
                } else {
                    condition = "sourceTable.%N·%M·entity.%N,"
                }

                add(
                    condition,
                    column.columnPropertyName,
                    MemberName("org.ktorm.dsl", "eq", true),
                    column.entityProperty.simpleName.asString()
                )
            }

            add(")\n»")
        }
    }

    private fun createExpression(): CodeBlock {
        val code = """
            val expression = database.dialect.createExpressionVisitor(%T).visit(
                %T(sourceTable.asExpression(), assignments, conditions.%M().asExpression())
            )
            
              
        """.trimIndent()

        return CodeBlock.of(
            code,
            AliasRemover::class.asClassName(),
            UpdateExpression::class.asClassName(),
            MemberName("org.ktorm.dsl", "combineConditions", true)
        )
    }
}
