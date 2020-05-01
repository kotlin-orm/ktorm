/*
 * Copyright 2018-2020 the original author or authors.
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
package me.liuwj.ktorm.jackson

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Created by beetlerx@gmail.com on May 1, 2020.
 *
 * [JacksonIgnore] is simple [com.fasterxml.jackson.annotation.JsonIgnore] support.
 * use this annotation to avoid property output to json
 *
 * @see com.fasterxml.jackson.annotation.JsonIgnore
 */
@MustBeDocumented
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class JacksonIgnore

/**
 * [JacksonProperty] is simple [com.fasterxml.jackson.annotation.JsonProperty] support.
 * use this annotation to control whether property output to json
 *
 * @see com.fasterxml.jackson.annotation.JsonProperty
 */
@MustBeDocumented
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class JacksonProperty(val access: JsonProperty.Access = JsonProperty.Access.AUTO)

/**
 * [JacksonAlias] is simple [com.fasterxml.jackson.annotation.JsonAlias] support.
 * use this annotation to set alias of property
 *
 * @see com.fasterxml.jackson.annotation.JsonAlias
 */
@MustBeDocumented
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class JacksonAlias(val value: String = "")
