/*
 * Copyright 2025 the original author or authors.
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
 *
 * Original authors of the snowflake dialect were CarGurus: Don Mitchell, Ashish Shrestha, Mike Roberts, and others.
 */
package org.ktorm.support.snowflake

import org.ktorm.dsl.window
import org.ktorm.expression.ScalarExpression
import org.ktorm.expression.WindowFunctionExpression
import org.ktorm.expression.WindowFunctionType
import org.ktorm.expression.WindowSpecificationExpression
import org.ktorm.schema.SqlType

public data class NullAwareWindowFunctionExpression<T : Any>(
    val type: WindowFunctionType,
    val arguments: List<ScalarExpression<*>>,
    val isDistinct: Boolean = false,
    val window: WindowSpecificationExpression = WindowSpecificationExpression(),
    override val sqlType: SqlType<T>,
    val handleNulls: NullHandling? = null,
    override val isLeafNode: Boolean = false,
    override val extraProperties: Map<String, Any> = emptyMap()
) : ScalarExpression<T>()

public enum class NullHandling {
    IGNORE_NULLS,
    RESPECT_NULLS
}

// These are manually enumerated from the Snowflake docs. Ktorm doesn't seem to expose an easy way
// to build static type checks for this condition.
private val HANDLE_NULLS_WINDOW_TYPES: Set<WindowFunctionType> = setOf(
    WindowFunctionType.FIRST_VALUE,
    WindowFunctionType.LAG,
    WindowFunctionType.LAST_VALUE,
    WindowFunctionType.LEAD,
    WindowFunctionType.NTH_VALUE,
)

public fun <T : Any> NullAwareWindowFunctionExpression<T>.over(
    window: WindowSpecificationExpression = window()
): NullAwareWindowFunctionExpression<T> {
    return this.copy(window = window)
}

/**
 *  Explicitly specify that the window function should ignore null values.
 */
@JvmName("last_value_ignore_nulls")
public fun <T : Any> WindowFunctionExpression<T>.ignoreNulls(): NullAwareWindowFunctionExpression<T> {
    require(this.type in HANDLE_NULLS_WINDOW_TYPES) {
        "Window function ${this.type} does not support IGNORE NULLS in Snowflake"
    }
    return NullAwareWindowFunctionExpression(
        type = type,
        arguments = arguments,
        isDistinct = isDistinct,
        window = window,
        handleNulls = NullHandling.IGNORE_NULLS,
        sqlType = sqlType,
        isLeafNode = isLeafNode,
        extraProperties = extraProperties,
    )
}

/**
 *  Explicitly specify that the window function should respect null values.
 */
@JvmName("last_value_respect_nulls")
public fun <T : Any> WindowFunctionExpression<T>.respectNulls(): NullAwareWindowFunctionExpression<T> {
    require(this.type in HANDLE_NULLS_WINDOW_TYPES) {
        "Window function ${this.type} does not support RESPECT NULLS in Snowflake"
    }
    return NullAwareWindowFunctionExpression(
        type = type,
        arguments = arguments,
        isDistinct = isDistinct,
        window = window,
        handleNulls = NullHandling.RESPECT_NULLS,
        sqlType = sqlType,
        isLeafNode = isLeafNode,
        extraProperties = extraProperties,
    )
}
