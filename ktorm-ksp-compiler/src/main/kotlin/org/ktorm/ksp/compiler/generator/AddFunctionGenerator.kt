/*
 * Copyright 2018-2024 the original author or authors.
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
import org.ktorm.expression.InsertExpression
import org.ktorm.ksp.spi.ColumnMetadata
import org.ktorm.ksp.spi.TableMetadata
import org.ktorm.schema.Column

@OptIn(KotlinPoetKspPreview::class)
internal object AddFunctionGenerator {

    fun generate(table: TableMetadata): FunSpec {
        val primaryKeys = table.columns.filter { it.isPrimaryKey }
        val useGeneratedKey = primaryKeys.size == 1 && primaryKeys[0].entityProperty.isMutable
        val entityClass = table.entityClass.toClassName()
        val tableClass = ClassName(table.entityClass.packageName.asString(), table.tableClassName)

        return FunSpec.builder("add")
            .addKdoc(kdoc(table, useGeneratedKey))
            .receiver(EntitySequence::class.asClassName().parameterizedBy(entityClass, tableClass))
            .addParameters(parameters(entityClass, useGeneratedKey))
            .returns(Int::class.asClassName())
            .addCode(checkForDml())
            .addCode(addValFun(table, useGeneratedKey))
            .addCode(addAssignments(table))
            .addCode(createExpression())
            .addCode(executeUpdate(useGeneratedKey, primaryKeys))
            .build()
    }

    private fun kdoc(table: TableMetadata, useGeneratedKey: Boolean): String {
        if (useGeneratedKey) {
            val pk = table.columns.single { it.isPrimaryKey }
            val pkName = table.entityClass.simpleName.asString() + "." + pk.entityProperty.simpleName.asString()
            return """
                Insert the given entity into the table that the sequence object represents.
                
                @param entity the entity to be inserted.
                @param isDynamic whether only non-null columns should be inserted.
                @param useGeneratedKey whether to obtain the generated primary key value and fill it into the property [$pkName] after insertion.
                @return the affected record number.
            """.trimIndent()
        } else {
            return """
                Insert the given entity into the table that the sequence object represents.
                
                @param entity the entity to be inserted.
                @param isDynamic whether only non-null columns should be inserted.
                @return the affected record number.
            """.trimIndent()
        }
    }

    private fun parameters(entityClass: ClassName, useGeneratedKey: Boolean): List<ParameterSpec> {
        if (useGeneratedKey) {
            return listOf(
                ParameterSpec.builder("entity", entityClass).build(),
                ParameterSpec.builder("isDynamic", typeNameOf<Boolean>()).defaultValue("false").build(),
                ParameterSpec.builder("useGeneratedKey", typeNameOf<Boolean>()).defaultValue("false").build()
            )
        } else {
            return listOf(
                ParameterSpec.builder("entity", entityClass).build(),
                ParameterSpec.builder("isDynamic", typeNameOf<Boolean>()).defaultValue("false").build()
            )
        }
    }

    internal fun checkForDml(): CodeBlock {
        val code = """
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
            
            
        """.trimIndent()

        return CodeBlock.of(code)
    }

    private fun addValFun(table: TableMetadata, useGeneratedKey: Boolean): CodeBlock {
        if (useGeneratedKey) {
            val pk = table.columns.single { it.isPrimaryKey }
            val code = """
                fun <T : Any> MutableList<%1T<*>>.addVal(column: %2T<T>, value: T?) {
                    if (useGeneratedKey && column === sourceTable.%3N) {
                        return
                    }
            
                    if (isDynamic && value == null) {
                        return
                    }
            
                    this += %1T(column.asExpression(), column.wrapArgument(value))
                }
                
                
            """.trimIndent()
            return CodeBlock.of(
                code,
                ColumnAssignmentExpression::class.asClassName(),
                Column::class.asClassName(),
                pk.columnPropertyName
            )
        } else {
            val code = """
                fun <T : Any> MutableList<%1T<*>>.addVal(column: %2T<T>, value: T?) {
                    if (isDynamic && value == null) {
                        return
                    }
            
                    this += %1T(column.asExpression(), column.wrapArgument(value))
                }
                
                
            """.trimIndent()
            return CodeBlock.of(code, ColumnAssignmentExpression::class.asClassName(), Column::class.asClassName())
        }
    }

    private fun addAssignments(table: TableMetadata): CodeBlock {
        return buildCodeBlock {
            addStatement("val assignments = ArrayList<%T<*>>()", ColumnAssignmentExpression::class.asClassName())

            for (column in table.columns) {
                addStatement(
                    "assignments.addVal(sourceTable.%N, entity.%N)",
                    column.columnPropertyName,
                    column.entityProperty.simpleName.asString()
                )
            }

            beginControlFlow("if (assignments.isEmpty())")
            addStatement("return 0")
            endControlFlow()

            add("\n")
        }
    }

    private fun createExpression(): CodeBlock {
        return buildCodeBlock {
            addStatement(
                "val visitor = database.dialect.createExpressionVisitor(%T)",
                AliasRemover::class.asClassName()
            )
            addStatement(
                "val expression = visitor.visit(%T(sourceTable.asExpression(), assignments))",
                InsertExpression::class.asClassName()
            )
        }
    }

    private fun executeUpdate(useGeneratedKey: Boolean, primaryKeys: List<ColumnMetadata>): CodeBlock {
        return buildCodeBlock {
            if (!useGeneratedKey) {
                addStatement("return database.executeUpdate(expression)")
            } else {
                beginControlFlow("if (!useGeneratedKey)")
                addStatement("return database.executeUpdate(expression)")
                nextControlFlow("else")
                addNamed(
                    format = """
                        val (effects, rowSet) = database.executeUpdateAndRetrieveKeys(expression)
                        if (rowSet.next()) {
                            val generatedKey = rowSet.%getGeneratedKey:M(sourceTable.%columnPropertyName:N)
                            if (generatedKey != null) {
                                if (database.logger.isDebugEnabled()) {
                                    database.logger.debug("Generated Key: ${'$'}generatedKey")
                                }
                                
                                entity.%propertyName:N = generatedKey
                            }
                        }
                        
                        return effects
                        
                    """.trimIndent(),

                    arguments = mapOf(
                        "propertyName" to primaryKeys[0].entityProperty.simpleName.asString(),
                        "columnPropertyName" to primaryKeys[0].columnPropertyName,
                        "getGeneratedKey" to MemberName("org.ktorm.dsl", "getGeneratedKey", true)
                    )
                )
                endControlFlow()
            }
        }
    }
}
