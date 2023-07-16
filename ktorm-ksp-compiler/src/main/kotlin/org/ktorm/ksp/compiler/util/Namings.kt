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

package org.ktorm.ksp.compiler.util

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import org.atteo.evo.inflector.English
import org.ktorm.ksp.spi.CodingNamingStrategy
import org.ktorm.ksp.spi.DatabaseNamingStrategy
import org.ktorm.ksp.spi.TableMetadata

internal object LowerSnakeCaseDatabaseNamingStrategy : DatabaseNamingStrategy {

    override fun getTableName(c: KSClassDeclaration): String {
        return CamelCase.toLowerSnakeCase(c.simpleName.asString())
    }

    override fun getColumnName(c: KSClassDeclaration, prop: KSPropertyDeclaration): String {
        return CamelCase.toLowerSnakeCase(prop.simpleName.asString())
    }

    override fun getRefColumnName(c: KSClassDeclaration, prop: KSPropertyDeclaration, ref: TableMetadata): String {
        val pk = ref.columns.single { it.isPrimaryKey }
        return CamelCase.toLowerSnakeCase(prop.simpleName.asString()) + "_" + pk.name
    }
}

internal object UpperSnakeCaseDatabaseNamingStrategy : DatabaseNamingStrategy {

    override fun getTableName(c: KSClassDeclaration): String {
        return CamelCase.toUpperSnakeCase(c.simpleName.asString())
    }

    override fun getColumnName(c: KSClassDeclaration, prop: KSPropertyDeclaration): String {
        return CamelCase.toUpperSnakeCase(prop.simpleName.asString())
    }

    override fun getRefColumnName(c: KSClassDeclaration, prop: KSPropertyDeclaration, ref: TableMetadata): String {
        val pk = ref.columns.single { it.isPrimaryKey }
        return CamelCase.toUpperSnakeCase(prop.simpleName.asString()) + "_" + pk.name
    }
}

internal object DefaultCodingNamingStrategy : CodingNamingStrategy {

    override fun getTableClassName(c: KSClassDeclaration): String {
        return English.plural(c.simpleName.asString())
    }

    override fun getEntitySequenceName(c: KSClassDeclaration): String {
        return CamelCase.toFirstLowerCamelCase(English.plural(c.simpleName.asString()))
    }

    override fun getColumnPropertyName(c: KSClassDeclaration, prop: KSPropertyDeclaration): String {
        return prop.simpleName.asString()
    }

    override fun getRefColumnPropertyName(
        c: KSClassDeclaration, prop: KSPropertyDeclaration, ref: TableMetadata
    ): String {
        val pk = ref.columns.single { it.isPrimaryKey }
        return prop.simpleName.asString() + pk.columnPropertyName.replaceFirstChar { it.uppercase() }
    }

    override fun getRefColumnPropertyNameForRefs(
        c: KSClassDeclaration, prop: KSPropertyDeclaration, ref: TableMetadata
    ): String {
        return prop.simpleName.asString()
    }
}

internal object CamelCase {
    // Matches boundaries between words, for example (abc|Def), (ABC|Def)
    private val boundaries = listOf(Regex("([a-z])([A-Z])"), Regex("([A-Z])([A-Z][a-z])"))

    fun toLowerSnakeCase(name: String): String {
        return boundaries.fold(name) { s, regex -> s.replace(regex, "$1_$2") }.lowercase()
    }

    fun toUpperSnakeCase(name: String): String {
        return boundaries.fold(name) { s, regex -> s.replace(regex, "$1_$2") }.uppercase()
    }

    fun toFirstLowerCamelCase(name: String): String {
        val i = boundaries.mapNotNull { regex -> regex.find(name) }.minOfOrNull { it.range.first } ?: 0
        return name.substring(0, i + 1).lowercase() + name.substring(i + 1)
    }
}
