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

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

/**
 * Naming strategy for Kotlin symbols in the generated code.
 */
public interface CodingNamingStrategy {

    /**
     * Generate the table class name.
     */
    public fun getTableClassName(c: KSClassDeclaration): String

    /**
     * Generate the entity sequence name.
     */
    public fun getEntitySequenceName(c: KSClassDeclaration): String

    /**
     * Generate the column property name.
     */
    public fun getColumnPropertyName(c: KSClassDeclaration, prop: KSPropertyDeclaration): String

    /**
     * Generate the reference column property name.
     */
    public fun getRefColumnPropertyName(c: KSClassDeclaration, prop: KSPropertyDeclaration, ref: TableMetadata): String

    /**
     * Generate the name of the referenced table property in the Refs wrapper class.
     */
    public fun getRefTablePropertyName(c: KSClassDeclaration, prop: KSPropertyDeclaration, ref: TableMetadata): String
}
