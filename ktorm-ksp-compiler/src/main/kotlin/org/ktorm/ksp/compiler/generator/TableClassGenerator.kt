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

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toClassName
import org.ktorm.dsl.QueryRowSet
import org.ktorm.ksp.compiler.util._type
import org.ktorm.ksp.compiler.util.getKotlinType
import org.ktorm.ksp.compiler.util.getRegisteringCodeBlock
import org.ktorm.ksp.spi.TableMetadata
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import org.ktorm.schema.Table

@OptIn(KotlinPoetKspPreview::class)
internal object TableClassGenerator {

    fun generate(table: TableMetadata, environment: SymbolProcessorEnvironment): TypeSpec {
        return TypeSpec.classBuilder(table.tableClassName)
            .addKdoc("Table %L. %L", table.name, table.entityClass.docString?.trimIndent().orEmpty())
            .addModifiers(KModifier.OPEN)
            .primaryConstructor(FunSpec.constructorBuilder().addParameter("alias", typeNameOf<String?>()).build())
            .configureSuperClass(table)
            .configureColumnProperties(table)
            .configureDoCreateEntityFunction(table, environment.options)
            .configureAliasedFunction(table)
            .configureCompanionObject(table)
            .build()
    }

    private fun TypeSpec.Builder.configureSuperClass(table: TableMetadata): TypeSpec.Builder {
        if (table.entityClass.classKind == ClassKind.INTERFACE) {
            superclass(Table::class.asClassName().parameterizedBy(table.entityClass.toClassName()))
        } else {
            superclass(BaseTable::class.asClassName().parameterizedBy(table.entityClass.toClassName()))
        }

        addSuperclassConstructorParameter("%S", table.name)
        addSuperclassConstructorParameter("alias")

        if (table.catalog != null) {
            addSuperclassConstructorParameter("catalog·=·%S", table.catalog!!)
        }

        if (table.schema != null) {
            addSuperclassConstructorParameter("schema·=·%S", table.schema!!)
        }

        return this
    }

    private fun TypeSpec.Builder.configureColumnProperties(table: TableMetadata): TypeSpec.Builder {
        for (column in table.columns) {
            val columnType = Column::class.asClassName().parameterizedBy(column.getKotlinType())
            val propertySpec = PropertySpec.builder(column.columnPropertyName, columnType)
                .addKdoc("Column %L. %L", column.name, column.entityProperty.docString?.trimIndent().orEmpty())
                .initializer(buildCodeBlock {
                    add(column.getRegisteringCodeBlock())

                    if (column.isPrimaryKey) {
                        add(".primaryKey()")
                    }

                    if (table.entityClass.classKind == ClassKind.INTERFACE) {
                        if (column.isReference) {
                            val pkg = column.referenceTable!!.entityClass.packageName.asString()
                            val name = column.referenceTable!!.tableClassName
                            val propName = column.entityProperty.simpleName.asString()
                            add(".references(%T)·{·it.%N·}", ClassName(pkg, name), propName)
                        } else {
                            add(".bindTo·{·it.%N·}", column.entityProperty.simpleName.asString())
                        }
                    }
                })
                .build()

            addProperty(propertySpec)
        }

        return this
    }

    private fun TypeSpec.Builder.configureDoCreateEntityFunction(
        table: TableMetadata, options: Map<String, String>
    ): TypeSpec.Builder {
        if (table.entityClass.classKind == ClassKind.INTERFACE) {
            return this
        }

        val func = FunSpec.builder("doCreateEntity")
            .addKdoc("Create an entity object from the specific row of query results.")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("row", QueryRowSet::class.asTypeName())
            .addParameter("withReferences", Boolean::class.asTypeName())
            .returns(table.entityClass.toClassName())
            .addCode(buildCodeBlock { buildDoCreateEntityFunctionBody(table, options) })
            .build()

        addFunction(func)
        return this
    }

    private fun CodeBlock.Builder.buildDoCreateEntityFunctionBody(table: TableMetadata, options: Map<String, String>) {
        val constructorParams = table.entityClass.primaryConstructor!!.parameters.associateBy { it.name!!.asString() }

        val hasDefaultValues = table.columns
            .mapNotNull { constructorParams[it.entityProperty.simpleName.asString()] }
            .any { it.hasDefault }

        if (hasDefaultValues && options["ktorm.allowReflection"] == "true") {
            createEntityByReflection(table, constructorParams)
        } else {
            createEntityByConstructor(table, constructorParams)
        }

        for (column in table.columns) {
            val propName = column.entityProperty.simpleName.asString()
            if (propName in constructorParams) {
                continue
            }

            if (column.entityProperty._type.isMarkedNullable) {
                addStatement("entity.%N·=·row[this.%N]", propName, column.columnPropertyName)
            } else {
                addStatement("entity.%N·=·row[this.%N]!!", propName, column.columnPropertyName)
            }
        }

        if (table.columns.any { it.entityProperty.simpleName.asString() !in constructorParams }) {
            addStatement("return·entity")
        }
    }

    private fun CodeBlock.Builder.createEntityByReflection(
        table: TableMetadata, constructorParams: Map<String, KSValueParameter>
    ) {
        addStatement(
            "val constructor = %T::class.%M!!",
            table.entityClass.toClassName(),
            MemberName("kotlin.reflect.full", "primaryConstructor", true)
        )

        add("«val args = mapOf(")

        for (column in table.columns) {
            val propName = column.entityProperty.simpleName.asString()
            if (propName in constructorParams) {
                add(
                    "constructor.%M(%S)!! to row[this.%N],",
                    MemberName("kotlin.reflect.full", "findParameterByName", true),
                    propName,
                    column.columnPropertyName
                )
            }
        }

        add(")\n»")
        addStatement("// Filter optional arguments out to make default values work.")

        if (table.columns.all { it.entityProperty.simpleName.asString() in constructorParams }) {
            addStatement("return constructor.callBy(args.filterNot { (k, v) -> k.isOptional && v == null })")
        } else {
            addStatement("val entity = constructor.callBy(args.filterNot { (k, v) -> k.isOptional && v == null })")
        }
    }

    private fun CodeBlock.Builder.createEntityByConstructor(
        table: TableMetadata, constructorParams: Map<String, KSValueParameter>
    ) {
        if (table.columns.all { it.entityProperty.simpleName.asString() in constructorParams }) {
            add("«return·%T(", table.entityClass.toClassName())
        } else {
            add("«val·entity·=·%T(", table.entityClass.toClassName())
        }

        for (column in table.columns) {
            val parameter = constructorParams[column.entityProperty.simpleName.asString()] ?: continue
            if (parameter._type.isMarkedNullable) {
                add("%N·=·row[this.%N],", parameter.name!!.asString(), column.columnPropertyName)
            } else {
                add("%N·=·row[this.%N]!!,", parameter.name!!.asString(), column.columnPropertyName)
            }
        }

        add(")\n»")
    }

    private fun TypeSpec.Builder.configureAliasedFunction(table: TableMetadata): TypeSpec.Builder {
        val kdoc = "" +
            "Return a new-created table object with all properties (including the table name and columns " +
            "and so on) being copied from this table, but applying a new alias given by the parameter."

        val func = FunSpec.builder("aliased")
            .addKdoc(kdoc)
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("alias", typeNameOf<String>())
            .returns(ClassName(table.entityClass.packageName.asString(), table.tableClassName))
            .addCode("return %L(alias)", table.tableClassName)
            .build()

        addFunction(func)
        return this
    }

    private fun TypeSpec.Builder.configureCompanionObject(table: TableMetadata): TypeSpec.Builder {
        val companion = TypeSpec.companionObjectBuilder(null)
            .addKdoc("The default table object of %L.", table.name)
            .superclass(ClassName(table.entityClass.packageName.asString(), table.tableClassName))
            .addSuperclassConstructorParameter(CodeBlock.of("alias·=·%S", table.alias))
            .build()

        addType(companion)
        return this
    }
}
