package org.ktorm.ksp.compiler.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import org.ktorm.ksp.spi.TableMetadata

/**
 * Created by vince at Jul 15, 2023.
 */
internal object RefsPropertyGenerator {

    fun generate(table: TableMetadata): PropertySpec {
        val tableClass = ClassName(table.entityClass.packageName.asString(), table.tableClassName)
        val refsClass = ClassName(table.entityClass.packageName.asString(), "${table.tableClassName}Refs")

        return PropertySpec.builder("refs", refsClass)
            .addKdoc("Return the refs object that provides a convenient way to access referenced tables.")
            .receiver(tableClass)
            .getter(FunSpec.getterBuilder().addStatement("return·%T(this)", refsClass).build())
            .build()
    }
}