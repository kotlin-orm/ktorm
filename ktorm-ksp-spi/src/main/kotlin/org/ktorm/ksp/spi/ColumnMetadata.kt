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

package org.ktorm.ksp.spi

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType

/**
 * Column definition metadata.
 */
public data class ColumnMetadata(

    /**
     * The annotated entity property of the column.
     */
    val entityProperty: KSPropertyDeclaration,

    /**
     * The belonging table.
     */
    val table: TableMetadata,

    /**
     * The name of the column.
     */
    val name: String,

    /**
     * Check if the column is a primary key.
     */
    val isPrimaryKey: Boolean,

    /**
     * The SQL type of the column.
     */
    val sqlType: KSType,

    /**
     * Check if the column is a reference column.
     */
    val isReference: Boolean,

    /**
     * The referenced table of the column.
     */
    val referenceTable: TableMetadata?,

    /**
     * The name of the corresponding column property in the generated table class.
     */
    val columnPropertyName: String,

    /**
     * The name of the corresponding referenced table property in the Refs wrapper class.
     */
    val refTablePropertyName: String?
)
