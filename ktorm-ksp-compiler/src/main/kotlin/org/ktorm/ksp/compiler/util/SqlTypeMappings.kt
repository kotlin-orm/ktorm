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

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import org.ktorm.ksp.api.EnumSqlTypeFactory
import org.ktorm.ksp.api.SqlTypeFactory
import org.ktorm.ksp.spi.ColumnMetadata
import org.ktorm.schema.*

internal fun KSPropertyDeclaration.getSqlType(resolver: Resolver): KSType? {
    val declaration = this.type.resolve().declaration as KSClassDeclaration
    if (declaration.classKind == ClassKind.ENUM_CLASS) {
        return resolver.getClassDeclarationByName<EnumSqlTypeFactory>()?.asType(emptyList())
    }

    val sqlType = when (declaration.qualifiedName?.asString()) {
        "kotlin.Boolean" -> BooleanSqlType::class
        "kotlin.Int" -> IntSqlType::class
        "kotlin.Short" -> ShortSqlType::class
        "kotlin.Long" -> LongSqlType::class
        "kotlin.Float" -> FloatSqlType::class
        "kotlin.Double" -> DoubleSqlType::class
        "kotlin.String" -> VarcharSqlType::class
        "kotlin.ByteArray" -> BytesSqlType::class
        "java.math.BigDecimal" -> DecimalSqlType::class
        "java.sql.Timestamp" -> TimestampSqlType::class
        "java.sql.Date" -> DateSqlType::class
        "java.sql.Time" -> TimeSqlType::class
        "java.time.Instant" -> InstantSqlType::class
        "java.time.LocalDateTime" -> LocalDateTimeSqlType::class
        "java.time.LocalDate" -> LocalDateSqlType::class
        "java.time.LocalTime" -> LocalTimeSqlType::class
        "java.time.MonthDay" -> MonthDaySqlType::class
        "java.time.YearMonth" -> YearMonthSqlType::class
        "java.time.Year" -> YearSqlType::class
        "java.util.UUID" -> UuidSqlType::class
        else -> null
    }

    return sqlType?.qualifiedName?.let { resolver.getClassDeclarationByName(it)?.asType(emptyList()) }
}

@OptIn(KotlinPoetKspPreview::class)
internal fun ColumnMetadata.getRegisteringCodeBlock(): CodeBlock {
    val sqlTypeName = sqlType.declaration.qualifiedName?.asString()
    val registerFun = when (sqlTypeName) {
        "org.ktorm.schema.BooleanSqlType" -> MemberName("org.ktorm.schema", "boolean", true)
        "org.ktorm.schema.IntSqlType" -> MemberName("org.ktorm.schema", "int", true)
        "org.ktorm.schema.ShortSqlType" -> MemberName("org.ktorm.schema", "short", true)
        "org.ktorm.schema.LongSqlType" -> MemberName("org.ktorm.schema", "long", true)
        "org.ktorm.schema.FloatSqlType" -> MemberName("org.ktorm.schema", "float", true)
        "org.ktorm.schema.DoubleSqlType" -> MemberName("org.ktorm.schema", "double", true)
        "org.ktorm.schema.DecimalSqlType" -> MemberName("org.ktorm.schema", "decimal", true)
        "org.ktorm.schema.VarcharSqlType" -> MemberName("org.ktorm.schema", "varchar", true)
        "org.ktorm.schema.TextSqlType" -> MemberName("org.ktorm.schema", "text", true)
        "org.ktorm.schema.BlobSqlType" -> MemberName("org.ktorm.schema", "blob", true)
        "org.ktorm.schema.BytesSqlType" -> MemberName("org.ktorm.schema", "bytes", true)
        "org.ktorm.schema.TimestampSqlType" -> MemberName("org.ktorm.schema", "jdbcTimestamp", true)
        "org.ktorm.schema.DateSqlType" -> MemberName("org.ktorm.schema", "jdbcDate", true)
        "org.ktorm.schema.TimeSqlType" -> MemberName("org.ktorm.schema", "jdbcTime", true)
        "org.ktorm.schema.InstantSqlType" -> MemberName("org.ktorm.schema", "timestamp", true)
        "org.ktorm.schema.LocalDateTimeSqlType" -> MemberName("org.ktorm.schema", "datetime", true)
        "org.ktorm.schema.LocalDateSqlType" -> MemberName("org.ktorm.schema", "date", true)
        "org.ktorm.schema.LocalTimeSqlType" -> MemberName("org.ktorm.schema", "time", true)
        "org.ktorm.schema.MonthDaySqlType" -> MemberName("org.ktorm.schema", "monthDay", true)
        "org.ktorm.schema.YearMonthSqlType" -> MemberName("org.ktorm.schema", "yearMonth", true)
        "org.ktorm.schema.YearSqlType" -> MemberName("org.ktorm.schema", "year", true)
        "org.ktorm.schema.UuidSqlType" -> MemberName("org.ktorm.schema", "uuid", true)
        else -> null
    }

    if (registerFun != null) {
        return CodeBlock.of("%M(%S)", registerFun, name)
    }

    if (sqlTypeName == "org.ktorm.ksp.api.EnumSqlTypeFactory") {
        return CodeBlock.of("%M<%T>(%S)", MemberName("org.ktorm.schema", "enum", true), getKotlinType(), name)
    }

    if (sqlTypeName == "org.ktorm.ksp.api.JsonSqlTypeFactory") {
        return CodeBlock.of("%M<%T>(%S)", MemberName("org.ktorm.jackson", "json", true), getKotlinType(), name)
    }

    val declaration = sqlType.declaration as KSClassDeclaration
    if (declaration.isSubclassOf<SqlType<*>>()) {
        return CodeBlock.of("registerColumn(%S,·%T)", name, sqlType.toTypeName())
    }

    if (declaration.isSubclassOf<SqlTypeFactory>()) {
        return CodeBlock.of(
            "registerColumn(%S,·%T.createSqlType(%T::%N))",
            name,
            sqlType.toTypeName(),
            table.entityClass.toClassName(),
            entityProperty.simpleName.asString()
        )
    }

    throw IllegalArgumentException("The sqlType class $sqlTypeName must be subtype of SqlType or SqlTypeFactory.")
}

@OptIn(KotlinPoetKspPreview::class)
internal fun ColumnMetadata.getKotlinType(): TypeName {
    if (isReference) {
        return referenceTable!!.columns.single { it.isPrimaryKey }.getKotlinType()
    } else {
        return entityProperty.type.resolve().makeNotNullable().toTypeName()
    }
}
