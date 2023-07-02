package org.ktorm.ksp.compiler.generator

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.typeNameOf
import org.ktorm.ksp.spi.ExtCodeGenerator
import org.ktorm.ksp.spi.TableMetadata

/**
 * Created by vince at May 27, 2023.
 */
@OptIn(KotlinPoetKspPreview::class)
class ExtCodeGeneratorTest : ExtCodeGenerator {

    override fun generateTypes(table: TableMetadata, environment: SymbolProcessorEnvironment): List<TypeSpec> {
        return listOf(TypeSpec.classBuilder("TestFor" + table.tableClassName).build())
    }

    override fun generateProperties(table: TableMetadata, environment: SymbolProcessorEnvironment): List<PropertySpec> {
        return listOf(
            PropertySpec.builder("pTest", typeNameOf<Int>())
                .addKdoc("This is a test property.")
                .receiver(table.entityClass.toClassName())
                .getter(FunSpec.getterBuilder().addStatement("return 0").build())
                .build()
        )
    }

    override fun generateFunctions(table: TableMetadata, environment: SymbolProcessorEnvironment): List<FunSpec> {
        return listOf(
            FunSpec.builder("fTest")
                .addKdoc("This is a test function. \n\n @since 3.6.0")
                .receiver(table.entityClass.toClassName())
                .returns(typeNameOf<Int>())
                .addStatement("return 0")
                .build()
        )
    }
}