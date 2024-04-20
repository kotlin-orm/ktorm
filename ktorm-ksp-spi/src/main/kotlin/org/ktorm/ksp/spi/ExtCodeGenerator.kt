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

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

/**
 * Ktorm KSP code generator interface for third-party extensions.
 */
public interface ExtCodeGenerator {

    /**
     * Generate types for the [table] in the corresponding resulting file.
     */
    public fun generateTypes(table: TableMetadata, environment: SymbolProcessorEnvironment): List<TypeSpec> {
        return emptyList()
    }

    /**
     * Generate top-level properties for the [table] in the corresponding resulting file.
     */
    public fun generateProperties(table: TableMetadata, environment: SymbolProcessorEnvironment): List<PropertySpec> {
        return emptyList()
    }

    /**
     * Generate top-level functions for the [table] in the corresponding resulting file.
     */
    public fun generateFunctions(table: TableMetadata, environment: SymbolProcessorEnvironment): List<FunSpec> {
        return emptyList()
    }
}
