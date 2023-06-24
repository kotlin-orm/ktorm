/*
 * Copyright 2022-2023 the original author or authors.
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

package org.ktorm.ksp.annotation

import org.ktorm.jackson.JsonSqlType
import org.ktorm.jackson.sharedObjectMapper
import org.ktorm.schema.EnumSqlType
import org.ktorm.schema.SqlType
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmErasure

/**
 * Type factory object that creates [EnumSqlType] instances.
 */
public object EnumSqlTypeFactory : SqlTypeFactory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> createSqlType(property: KProperty1<*, T?>): SqlType<T> {
        val returnType = property.returnType.jvmErasure.java
        if (returnType.isEnum) {
            return EnumSqlType(returnType as Class<out Enum<*>>) as SqlType<T>
        } else {
            throw IllegalArgumentException("The property is required to be typed of enum but actually: $returnType")
        }
    }
}

/**
 * Type factory object that creates [JsonSqlType] instances.
 */
public object JsonSqlTypeFactory : SqlTypeFactory {

    override fun <T : Any> createSqlType(property: KProperty1<*, T?>): SqlType<T> {
        return JsonSqlType(sharedObjectMapper, sharedObjectMapper.constructType(property.returnType.javaType))
    }
}
