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

package org.ktorm.ksp.compiler.parser

import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.ClassKind.*
import org.ktorm.entity.Entity
import org.ktorm.ksp.annotation.*
import org.ktorm.ksp.compiler.util.*
import org.ktorm.ksp.spi.CodingNamingStrategy
import org.ktorm.ksp.spi.ColumnMetadata
import org.ktorm.ksp.spi.DatabaseNamingStrategy
import org.ktorm.ksp.spi.TableMetadata
import org.ktorm.schema.TypeReference
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toClassName
import java.lang.reflect.InvocationTargetException
import java.util.*
import kotlin.reflect.jvm.jvmName

@OptIn(KspExperimental::class)
internal class MetadataParser(resolver: Resolver, environment: SymbolProcessorEnvironment) {
    private val _resolver = resolver
    private val _options = environment.options
    private val _logger = environment.logger
    private val _databaseNamingStrategy = loadDatabaseNamingStrategy()
    private val _codingNamingStrategy = loadCodingNamingStrategy()
    private val _tablesCache = HashMap<String, TableMetadata>()

    @Suppress("SwallowedException")
    private fun loadDatabaseNamingStrategy(): DatabaseNamingStrategy {
        val name = _options["ktorm.dbNamingStrategy"] ?: "lower-snake-case"
        if (name == "lower-snake-case") {
            return LowerSnakeCaseDatabaseNamingStrategy
        }
        if (name == "upper-snake-case") {
            return UpperSnakeCaseDatabaseNamingStrategy
        }

        try {
            val cls = Class.forName(name)
            return (cls.kotlin.objectInstance ?: cls.getDeclaredConstructor().newInstance()) as DatabaseNamingStrategy
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }

    @Suppress("SwallowedException")
    private fun loadCodingNamingStrategy(): CodingNamingStrategy {
        val name = _options["ktorm.codingNamingStrategy"] ?: return DefaultCodingNamingStrategy

        try {
            val cls = Class.forName(name)
            return (cls.kotlin.objectInstance ?: cls.getDeclaredConstructor().newInstance()) as CodingNamingStrategy
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }

    fun parseTableMetadata(cls: KSClassDeclaration): TableMetadata {
        val className = cls.qualifiedName!!.asString()
        val r = _tablesCache[className]
        if (r != null) {
            return r
        }

        if (cls.classKind != CLASS && cls.classKind != INTERFACE) {
            throw IllegalStateException("$className should be a class or interface but actually ${cls.classKind}.")
        }

        if (cls.classKind == INTERFACE && !cls.isSubclassOf<Entity<*>>()) {
            throw IllegalStateException("$className should extend from org.ktorm.entity.Entity.")
        }

        if (cls.classKind == CLASS && cls.isAbstract()) {
            throw IllegalStateException("$className cannot be an abstract class.")
        }

        _logger.info("[ktorm-ksp-compiler] parse table metadata from entity: $className")
        val table = cls.getAnnotationsByType(Table::class).first()
        val (superClass, superTableClasses) = parseSuperTableClass(cls)
        val allPropertyNamesOfSuperTables = superTableClasses.flatMap { it.getProperties(emptySet()) }.map { it.simpleName.asString() }

        val tableMetadata = TableMetadata(
            entityClass = cls,
            name = table.name.ifEmpty { _databaseNamingStrategy.getTableName(cls) },
            alias = table.alias.takeIf { it.isNotEmpty() },
            catalog = table.catalog.ifEmpty { _options["ktorm.catalog"] }?.takeIf { it.isNotEmpty() },
            schema = table.schema.ifEmpty { _options["ktorm.schema"] }?.takeIf { it.isNotEmpty() },
            tableClassName = table.className.ifEmpty { _codingNamingStrategy.getTableClassName(cls) },
            entitySequenceName = table.entitySequenceName.ifEmpty { _codingNamingStrategy.getEntitySequenceName(cls) },
            ignoreProperties = table.ignoreProperties.toSet() + allPropertyNamesOfSuperTables, // ignore properties of super tables
            columns = ArrayList(),
            superClass = superClass
        )

        val columns = tableMetadata.columns as MutableList
        for (property in cls.getProperties(tableMetadata.ignoreProperties)) {
            if (property.isAnnotationPresent(References::class)) {
                columns += parseRefColumnMetadata(property, tableMetadata)
            } else {
                columns += parseColumnMetadata(property, tableMetadata)
            }
        }

        _tablesCache[className] = tableMetadata
        return tableMetadata
    }

    private fun KSClassDeclaration.getProperties(ignoreProperties: Set<String>): Sequence<KSPropertyDeclaration> {
        val constructorParams = HashSet<String>()
        if (classKind == CLASS) {
            primaryConstructor?.parameters?.mapTo(constructorParams) { it.name!!.asString() }
        }

        return this.getAllProperties()
            .filterNot { it.simpleName.asString() in ignoreProperties }
            .filterNot { it.isAnnotationPresent(Ignore::class) }
            .filterNot { classKind == CLASS && !it.hasBackingField }
            .filterNot { classKind == INTERFACE && !it.isAbstract() }
            .filterNot { classKind == INTERFACE && it.simpleName.asString() in setOf("entityClass", "properties") }
            .sortedByDescending { it.simpleName.asString() in constructorParams }
    }

    private fun parseColumnMetadata(property: KSPropertyDeclaration, table: TableMetadata): ColumnMetadata {
        // @Column annotation is optional.
        val column = property.getAnnotationsByType(Column::class).firstOrNull()

        var name = column?.name
        if (name.isNullOrEmpty()) {
            name = _databaseNamingStrategy.getColumnName(table.entityClass, property)
        }

        var propertyName = column?.propertyName
        if (propertyName.isNullOrEmpty()) {
            propertyName = _codingNamingStrategy.getColumnPropertyName(table.entityClass, property)
        }

        return ColumnMetadata(
            entityProperty = property,
            table = table,
            name = name,
            isPrimaryKey = property.isAnnotationPresent(PrimaryKey::class),
            sqlType = parseColumnSqlType(property),
            isReference = false,
            referenceTable = null,
            columnPropertyName = propertyName,
            refTablePropertyName = null
        )
    }

    private fun parseColumnSqlType(property: KSPropertyDeclaration): KSType {
        val propName = property.qualifiedName?.asString()
        var sqlType = property.annotations
            .filter { it._annotationType.declaration.qualifiedName?.asString() == Column::class.jvmName }
            .flatMap { it.arguments }
            .filter { it.name?.asString() == Column::sqlType.name }
            .map { it.value as KSType? }
            .singleOrNull()

        if (sqlType?.declaration?.qualifiedName?.asString() == Nothing::class.jvmName) {
            sqlType = null
        }

        if (sqlType == null) {
            sqlType = property.getSqlType(_resolver)
        }

        if (sqlType == null) {
            val msg = "Parse sqlType error for property $propName: cannot infer sqlType, please specify manually."
            throw IllegalArgumentException(msg)
        }

        val declaration = sqlType.declaration as KSClassDeclaration
        if (declaration.classKind != OBJECT) {
            if (declaration.isAbstract()) {
                val msg = "Parse sqlType error for property $propName: the sqlType class cannot be abstract."
                throw IllegalArgumentException(msg)
            }

            val hasConstructor = declaration.getConstructors()
                .filter { it.parameters.size == 1 }
                .filter { it.parameters[0]._type.declaration.qualifiedName?.asString() == TypeReference::class.jvmName }
                .any()

            if (!hasConstructor) {
                val msg = "" +
                    "Parse sqlType error for property $propName: the sqlType should be a Kotlin singleton object or " +
                    "a normal class with a constructor that accepts a single org.ktorm.schema.TypeReference argument."
                throw IllegalArgumentException(msg)
            }
        }

        return sqlType
    }

    private fun parseRefColumnMetadata(property: KSPropertyDeclaration, table: TableMetadata): ColumnMetadata {
        val propName = property.qualifiedName?.asString()
        if (property.isAnnotationPresent(Column::class)) {
            val msg = "Parse ref column error for property $propName: @Column and @References cannot be used together."
            throw IllegalStateException(msg)
        }

        if (table.entityClass.classKind != INTERFACE) {
            val msg =
                "Parse ref column error for property $propName: @References only allowed in interface-based entities."
            throw IllegalStateException(msg)
        }

        val refEntityClass = property._type.declaration as KSClassDeclaration
        table.checkCircularRef(refEntityClass)

        if (refEntityClass.classKind != INTERFACE) {
            val msg = "Parse ref column error for property $propName: the referenced entity class must be an interface."
            throw IllegalStateException(msg)
        }

        if (!refEntityClass.isAnnotationPresent(Table::class)) {
            val msg =
                "Parse ref column error for property $propName: the referenced entity must be annotated with @Table."
            throw IllegalStateException(msg)
        }

        val refTable = parseTableMetadata(refEntityClass)
        val primaryKeys = refTable.columns.filter { it.isPrimaryKey }
        if (primaryKeys.isEmpty()) {
            val msg = "Parse ref column error for property $propName: the referenced table doesn't have a primary key."
            throw IllegalStateException(msg)
        }

        if (primaryKeys.size > 1) {
            val msg =
                "Parse ref column error for property $propName: the referenced table cannot have compound primary keys."
            throw IllegalStateException(msg)
        }

        val reference = property.getAnnotationsByType(References::class).first()
        var name = reference.name
        if (name.isEmpty()) {
            name = _databaseNamingStrategy.getRefColumnName(table.entityClass, property, refTable)
        }

        var propertyName = reference.propertyName
        if (propertyName.isEmpty()) {
            propertyName = _codingNamingStrategy.getRefColumnPropertyName(table.entityClass, property, refTable)
        }

        var refTablePropertyName = reference.refTablePropertyName
        if (refTablePropertyName.isEmpty()) {
            refTablePropertyName = _codingNamingStrategy.getRefTablePropertyName(table.entityClass, property, refTable)
        }

        return ColumnMetadata(
            entityProperty = property,
            table = table,
            name = name,
            isPrimaryKey = property.isAnnotationPresent(PrimaryKey::class),
            sqlType = primaryKeys[0].sqlType,
            isReference = true,
            referenceTable = refTable,
            columnPropertyName = propertyName,
            refTablePropertyName = refTablePropertyName
        )
    }

    /**
     * @return the super table class and all class be annotated with [SuperTableClass] in the inheritance hierarchy.
     */
    @OptIn(KotlinPoetKspPreview::class)
    private fun parseSuperTableClass(cls: KSClassDeclaration): Pair<ClassName, Set<KSClassDeclaration>> {
        val superTableClassAnnPair = cls.findAllAnnotationsInInheritanceHierarchy(SuperTableClass::class.qualifiedName!!)

        // if there is no SuperTableClass annotation, return the default super table class based on the class kind.
        if (superTableClassAnnPair.isEmpty()) {
            return if (cls.classKind == INTERFACE) {
                org.ktorm.schema.Table::class.asClassName() to emptySet()
            } else {
                org.ktorm.schema.BaseTable::class.asClassName() to emptySet()
            }
        }

        // SuperTableClass annotation can only be used on interface
        if (superTableClassAnnPair.map { it.first }.any { it.classKind != INTERFACE }) {
            val msg = "SuperTableClass annotation can only be used on interface."
            throw IllegalArgumentException(msg)
        }

        // find the last annotation in the inheritance hierarchy
        val superTableClasses = superTableClassAnnPair
            .map { it.second }
            .map { it.arguments.single { it.name?.asString() == SuperTableClass::value.name } }
            .map { it.value as KSType }
            .map { it.declaration as KSClassDeclaration }

        var lowestSubClass = superTableClasses.first()
        for (i in 1 until superTableClasses.size) {
            val cur = superTableClasses[i]
            if (cur.isSubclassOf(lowestSubClass)) {
                lowestSubClass = cur
            } else if (!lowestSubClass.isSubclassOf(cur)) {
                val msg =
                    "There are multiple SuperTableClass annotations in the inheritance hierarchy of class ${cls.qualifiedName?.asString()}," +
                            "but the values of annotation are not in the same inheritance hierarchy."
                throw IllegalArgumentException(msg)
            }
        }
        //TODO: to check All constructor parameters owned by `BaseTable` should also be owned by lowestSubClass.
        return lowestSubClass.toClassName() to superTableClasses.toSet()
    }

    private fun TableMetadata.checkCircularRef(ref: KSClassDeclaration, stack: LinkedList<String> = LinkedList()) {
        val className = this.entityClass.qualifiedName?.asString()
        val refClassName = ref.qualifiedName?.asString()

        stack.push(refClassName)

        if (className == refClassName) {
            val route = stack.asReversed().joinToString(separator = " --> ")
            val msg = "Circular reference is not allowed, current table: $className, reference route: $route."
            throw IllegalStateException(msg)
        }

        val refTable = ref.getAnnotationsByType(Table::class).firstOrNull()
        for (property in ref.getProperties(refTable?.ignoreProperties?.toSet() ?: emptySet())) {
            if (property.isAnnotationPresent(References::class)) {
                val propType = property._type.declaration as KSClassDeclaration
                checkCircularRef(propType, stack)
            }
        }

        stack.pop()
    }
}
