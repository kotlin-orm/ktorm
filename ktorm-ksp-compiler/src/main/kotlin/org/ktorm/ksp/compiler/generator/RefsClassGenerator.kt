package org.ktorm.ksp.compiler.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import org.ktorm.ksp.spi.TableMetadata

/**
 * Created by vince at Jul 15, 2023.
 */
internal object RefsClassGenerator {

    fun generate(table: TableMetadata): TypeSpec {
        val tableClass = ClassName(table.entityClass.packageName.asString(), table.tableClassName)
        return TypeSpec.classBuilder("${table.tableClassName}Refs")
            .addKdoc("Wrapper class that provides a convenient way to access reference tables.")
            .primaryConstructor(FunSpec.constructorBuilder().addParameter("t", tableClass).build())
            .build()
    }
}