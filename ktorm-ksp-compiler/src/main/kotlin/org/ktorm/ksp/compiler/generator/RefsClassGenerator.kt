package org.ktorm.ksp.compiler.generator

import com.squareup.kotlinpoet.*
import org.ktorm.ksp.spi.TableMetadata

/**
 * Created by vince at Jul 15, 2023.
 */
internal object RefsClassGenerator {

    fun generate(table: TableMetadata): TypeSpec {
        val tableClass = ClassName(table.entityClass.packageName.asString(), table.tableClassName)

        val typeSpec = TypeSpec.classBuilder("${table.tableClassName}Refs")
            .addKdoc("Wrapper class that provides a convenient way to access referenced tables.")
            .primaryConstructor(FunSpec.constructorBuilder().addParameter("t", tableClass).build())

        for (column in table.columns) {
            val refTable = column.referenceTable ?: continue
            val refTableClass = ClassName(refTable.entityClass.packageName.asString(), refTable.tableClassName)

            val propertySpec = PropertySpec.builder(column.refTablePropertyName!!, refTableClass)
                .addKdoc("Return the referenced table [${refTable.tableClassName}].")
                .initializer(CodeBlock.of("t.%N.referenceTable as %T", column.columnPropertyName, refTableClass))
                .build()

            typeSpec.addProperty(propertySpec)
        }

        return typeSpec.build()
    }
}