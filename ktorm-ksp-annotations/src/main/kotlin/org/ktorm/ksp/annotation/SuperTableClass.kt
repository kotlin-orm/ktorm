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

package org.ktorm.ksp.annotation

import org.ktorm.schema.BaseTable
import kotlin.reflect.KClass

/**
 * Be used on Entity interface, to specify the super table class for the table class generated.
 * @property value the super table class, which should be a subclass of `org.ktorm.schema.BaseTable`
 * the `tableName` and `alias` parameters be required on primary constructor, other parameters are optional.
 *
 * if not specified, the super table class will be determined by the kind of the entity class.
 * `org.ktorm.schema.Table` for interface, `org.ktorm.schema.BaseTable` for class.
 *
 * If there are multiple `SuperTableClass` on the inheritance hierarchy of entity,
 * and they have an inheritance relationship, the super table class will be the last one of them.
 * and they don't have an inheritance relationship, an error will be reported.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class SuperTableClass(
    public val value: KClass<out BaseTable<out Any>>
)
