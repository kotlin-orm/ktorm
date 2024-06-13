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

package org.ktorm.ksp.spi

import com.google.devtools.ksp.symbol.KSClassDeclaration
import kotlin.reflect.KClass

/**
 * Table definition metadata.
 */
public data class TableMetadata(

    /**
     * The annotated entity class of the table.
     */
    val entityClass: KSClassDeclaration,

    /**
     * The name of the table.
     */
    val name: String,

    /**
     * The alias of the table.
     */
    val alias: String?,

    /**
     * The catalog of the table.
     */
    val catalog: String?,

    /**
     * The schema of the table.
     */
    val schema: String?,

    /**
     * The name of the corresponding table class in the generated code.
     */
    val tableClassName: String,

    /**
     * The name of the corresponding entity sequence in the generated code.
     */
    val entitySequenceName: String,

    /**
     * Properties that should be ignored for generating column definitions.
     */
    val ignoreProperties: Set<String>,

    /**
     * Columns in the table.
     */
    val columns: List<ColumnMetadata>,

    /**
     * The super class of the table class in the generated code.
     */
    val superClass: KClass<*>
)
