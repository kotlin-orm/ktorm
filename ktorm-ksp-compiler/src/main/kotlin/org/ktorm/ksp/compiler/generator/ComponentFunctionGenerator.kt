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

import com.google.devtools.ksp.isAbstract
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import org.ktorm.ksp.compiler.util._type
import org.ktorm.ksp.spi.TableMetadata

@OptIn(KotlinPoetKspPreview::class)
internal object ComponentFunctionGenerator {

    fun generate(table: TableMetadata): Sequence<FunSpec> {
        return table.entityClass.getAllProperties()
            .filter { it.isAbstract() }
            .filterNot { it.simpleName.asString() in setOf("entityClass", "properties") }
            .mapIndexed { i, prop ->
                FunSpec.builder("component${i + 1}")
                    .addKdoc("Return the value of [%L.%L]. ",
                        table.entityClass.simpleName.asString(), prop.simpleName.asString())
                    .addModifiers(KModifier.OPERATOR)
                    .receiver(table.entityClass.toClassName())
                    .returns(prop._type.toTypeName())
                    .addCode("return·this.%N", prop.simpleName.asString())
                    .build()
            }
    }
}
