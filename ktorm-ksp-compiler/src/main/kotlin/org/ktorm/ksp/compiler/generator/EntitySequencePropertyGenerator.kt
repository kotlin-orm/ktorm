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
import org.ktorm.database.Database
import org.ktorm.entity.EntitySequence
import org.ktorm.ksp.spi.TableMetadata

@OptIn(KotlinPoetKspPreview::class)
internal object EntitySequencePropertyGenerator {

    fun generate(table: TableMetadata): PropertySpec {
        val entityClass = table.entityClass.toClassName()
        val tableClass = ClassName(table.entityClass.packageName.asString(), table.tableClassName)
        val entitySequenceType = EntitySequence::class.asClassName().parameterizedBy(entityClass, tableClass)

        return PropertySpec.builder(table.entitySequenceName, entitySequenceType)
            .addKdoc("Return the default entity sequence of [%L].", table.tableClassName)
            .receiver(Database::class.asClassName())
            .getter(
                FunSpec.getterBuilder()
                    .addStatement(
                        "return·this.%M(%N)",
                        MemberName("org.ktorm.entity", "sequenceOf", true),
                        table.tableClassName)
                    .build())
            .build()
    }
}
