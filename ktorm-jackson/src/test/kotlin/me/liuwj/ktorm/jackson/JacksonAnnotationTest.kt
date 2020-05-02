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

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.readValue
import me.liuwj.ktorm.entity.Entity
import org.junit.Test


class JacksonAnnotationTest {
    private val objectMapper = ObjectMapper()
            .configure(SerializationFeature.INDENT_OUTPUT, true)
            .findAndRegisterModules()

    interface TestEntity : Entity<TestEntity> {
        companion object : Entity.Factory<TestEntity>()
        var id: Int
        var name: String
        var nullInJson: String?
        @get:JsonIgnore
        @set:JsonIgnore
        var toIgnore: String
        @set:JsonProperty(access = JsonProperty.Access.READ_ONLY)
        var readOnly: String
        @get:JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        var writeOnly: String
        @get:JsonProperty("jacksonAlias")
        @set:JsonProperty("jacksonAlias")
        @set:JsonAlias(value = ["alias1", "alias2"])
        var alias: String
    }

    class TestDefaultClass {
        var nullInJson: String? = null
        @get:JsonIgnore
        @set:JsonIgnore
        var toIgnore: String = "toIgnore"
        @set:JsonProperty(access = JsonProperty.Access.READ_ONLY)
        var readOnly: String = "default value"
        @get:JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        var writeOnly: String = "writeOnly"
        @get:JsonProperty("jacksonAlias")
        @set:JsonAlias(value = ["jacksonAlias1", "jacksonAlias2"])
        var alias: String = "alias"
        override fun toString(): String {
            return "TestJavaClass(nullInJson=$nullInJson, readOnly='$readOnly', writeOnly='$writeOnly', alias='$alias')"
        }

    }

    @Test
    fun `default jackson serialize`() {
        println(objectMapper.writeValueAsString(TestDefaultClass()))
    }

    @Test
    fun `default jackson deserialize#1`() {
        val s = """
            {
             
              "jacksonAlias2" : "this is alias for 'alias'#2",
              "jacksonAlias" : "this is alias for 'alias'#1",
              "nullInJson" : null,
              "readOnly" : "test readOnly",
              "writeOnly" : "test writeOnly"
            }
        """
        // alias='this is alias for 'alias'#2
        println(objectMapper.readValue(s, TestDefaultClass::class.java))
    }

    @Test
    fun `default jackson deserialize#2`() {
        val s = """
            {
              "jacksonAlias2" : "this is alias for 'alias'#2",
              "jacksonAlias1" : "this is alias for 'alias'#1",
              "nullInJson" : null,
              "readOnly" : "readOnly",
              "writeOnly" : "test writeOnly"
            }
        """
        // alias='this is alias for 'alias'#1
        println(objectMapper.readValue(s, TestDefaultClass::class.java))
    }

    @Test
    fun testSerialize() {
        val testEntity = TestEntity {
            id = 42
            name = "TestEntity"
            toIgnore = "you won't see this in json"
            readOnly = "readOnly"
            writeOnly = "writeOnly (you won't see this either)"
            nullInJson = null
            alias = "this key should be jacksonAlias"
        }
        println(objectMapper.writeValueAsString(testEntity))
        val map = objectMapper.convertValue(testEntity, Map::class.java)
        assert(map["id"] == 42)
        assert(map["name"] == "TestEntity")
        assert(!map.contains("toIgnore"))
        assert(!map.contains("writeOnly"))
        assert(map["readOnly"] == "readOnly")
        assert(map.contains("nullInJson") && map["nullInJson"] == null)
        assert(map["jacksonAlias"] == "this key should be jacksonAlias")
    }

    @Test
    fun `test default deserialize`() {
        val json = """
            {
              "id" : 42,
              "name" : "TestEntity",
              "jacksonAlias" : "this key should be alias",
              "nullInJson" : null,
              "readOnly" : "readOnly",
              "writeOnly": "writeOnly",
              "toIgnore": "this is empty in entity"
            }
        """
        val entity = objectMapper.readValue<TestEntity>(json)
        println(entity)
        assert(entity.id == 42)
        assert(entity.name == "TestEntity")
        assert(entity.alias == "this key should be alias")
        assert(entity.nullInJson == null)
        assert(entity.readOnly.isEmpty())
        assert(entity.writeOnly == "writeOnly")
        assert(entity.toIgnore.isEmpty())
    }

    @Test
    fun `test deserialize with alias name` () {
        val json = """
            {
              "id" : 42,
              "name" : "TestEntity",
              "alias1" : "this is alias for 'alias'#1",
              "nullInJson" : null,
              "readOnly" : "readOnly",
              "writeOnly": "writeOnly",
              "toIgnore": "this is empty in entity"
            }
        """
        val entity = objectMapper.readValue(json, TestEntity::class.java)
        println(entity)
        assert(entity.id == 42)
        assert(entity.name == "TestEntity")
        assert(entity.alias == "this is alias for 'alias'#1")
        assert(entity.nullInJson == null)
        assert(entity.readOnly.isEmpty())
        assert(entity.writeOnly == "writeOnly")
        assert(entity.toIgnore.isEmpty())
    }


    @Test
    fun `test deserialize multi alias name` () {
        var json = """
            {
              "alias2" : "this is alias for 'alias'#alias2",
              "alias1" : "this is alias for 'alias'#alias1"
            }
        """
        var testEntity = objectMapper.readValue(json, TestEntity::class.java)
        println(testEntity)
        assert(testEntity.alias == "this is alias for 'alias'#alias1")

        json = """
            {
              "alias1" : "this is alias for 'alias'#alias1",
              "alias2" : "this is alias for 'alias'#alias2",
              "jacksonAlias" : "this is alias for 'alias'#jacksonAlias"
            }
        """
        testEntity = objectMapper.readValue(json, TestEntity::class.java)
        println(testEntity)
        assert(testEntity.alias == "this is alias for 'alias'#jacksonAlias")
    }
}