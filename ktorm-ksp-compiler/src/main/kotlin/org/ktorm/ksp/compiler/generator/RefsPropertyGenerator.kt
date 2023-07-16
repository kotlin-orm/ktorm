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
