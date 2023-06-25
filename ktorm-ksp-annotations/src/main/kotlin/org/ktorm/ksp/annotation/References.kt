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

/**
 * Specify the mapped column for an entity property, and bind this column to a reference table. Typically,
 * this column is a foreign key in relational databases. Entity sequence APIs would automatically left-join
 * all references (recursively) by default.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
public annotation class References(

    /**
     * The name of the column.
     *
     * If not specified, the name will be generated by a naming strategy. This naming strategy can be configured
     * by KSP option `ktorm.dbNamingStrategy`, which accepts the following values:
     *
     * - lower-snake-case (default): generate names in lower snake-case, the names are concatenation of the current
     * property's name and the referenced table's primary key name. For example, a property `user` referencing a table
     * that has a primary key named `id` will get a name `user_id`.
     *
     * - upper-snake-case: generate names in upper snake-case, the names are concatenation of the current
     * property's name and the referenced table's primary key name. For example, a property `user` referencing a table
     * that has a primary key named `id` will get a name `USER_ID`.
     *
     * - Class name of a custom naming strategy, which should be an implementation of
     * `org.ktorm.ksp.spi.DatabaseNamingStrategy`.
     */
    val name: String = "",

    /**
     * The name of the corresponding column property in the generated table class.
     *
     * If not specified, the name will be the concatenation of the current property's name and the referenced table's
     * primary key name. This behavior can be configured by KSP option `ktorm.codingNamingStrategy`, which accepts
     * an implementation class name of `org.ktorm.ksp.spi.CodingNamingStrategy`.
     */
    val propertyName: String = "",
)