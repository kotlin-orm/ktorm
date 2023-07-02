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

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toClassName
import org.ktorm.ksp.spi.TableMetadata

@OptIn(KotlinPoetKspPreview::class)
internal object CopyFunctionGenerator {

    fun generate(table: TableMetadata): FunSpec {
        return FunSpec.builder("copy")
            .addKdoc(
                "Return a deep copy of this entity (which has the same property values and tracked statuses), " +
                "and alter the specified property values. "
            )
            .receiver(table.entityClass.toClassName())
            .addParameters(PseudoConstructorFunctionGenerator.buildParameters(table).asIterable())
            .returns(table.entityClass.toClassName())
            .addCode(PseudoConstructorFunctionGenerator.buildFunctionBody(table, isCopy = true))
            .build()
    }
}
