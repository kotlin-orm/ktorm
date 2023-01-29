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

package org.ktorm.entity

import org.ktorm.schema.ColumnBinding

/**
 * Entity extension APIs.
 *
 * Note these APIs are designed to be used by Ktorm's 3rd party extensions, applications should not use them directly.
 *
 * @since 3.5.0
 */
public class EntityExtensionsApi {

    /**
     * Check if the specific column value exists in this entity.
     *
     * Please keep in mind that null is also a valid column value, so if a column value was set to null, this function
     * returns true.
     */
    public fun Entity<*>.hasColumnValue(binding: ColumnBinding): Boolean {
        return implementation.hasColumnValue(binding)
    }

    /**
     * Get the specific column value from this entity, returning null if the value doesn't exist.
     */
    public fun Entity<*>.getColumnValue(binding: ColumnBinding): Any? {
        return implementation.getColumnValue(binding)
    }

    /**
     * Set the specific column's value into this entity.
     */
    public fun Entity<*>.setColumnValue(binding: ColumnBinding, value: Any?) {
        implementation.setColumnValue(binding, value)
    }
}
