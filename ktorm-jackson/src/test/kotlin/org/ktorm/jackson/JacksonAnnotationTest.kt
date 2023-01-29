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
package org.ktorm.jackson

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.Test
import org.ktorm.entity.Entity

/**
 * Created by beetlerx on May 01, 2020.
 */
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
    fun defaultJacksonSerialize() {
        println(objectMapper.writeValueAsString(TestDefaultClass()))
    }

    @Test
    fun defaultJacksonDeserialize1() {
        val s = """
            {
             
              "jacksonAlias2" : "this is alias for 'alias'#2",
              "jacksonAlias" : "this is alias for 'alias'#1",
              "nullInJson" : null,
              "readOnly" : "test readOnly",
              "writeOnly" : "test writeOnly"
            }
        """
        // alias = this is alias for 'alias'#1
        println(objectMapper.readValue(s, TestDefaultClass::class.java))
    }

    /**
     * this defaultJacksonDeserialize2 and defaultJacksonDeserialize1 above
     * shows that jackson inject field value with multi alias according to the order in the json text,
     * and jackson takes the last one. We'll use this result later.
     */
    @Test
    fun defaultJacksonDeserialize2() {
        val s = """
            {
              "jacksonAlias2" : "this is alias for 'alias'#2",
              "jacksonAlias1" : "this is alias for 'alias'#1",
              "nullInJson" : null,
              "readOnly" : "readOnly",
              "writeOnly" : "test writeOnly"
            }
        """
        // alias = this is alias for 'alias'#1
        // readOnly = default value
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
    fun testDefaultDeserialize() {
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
    fun testDeserializeWithAliasName () {
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
    fun testDeserializeMultiAliasName() {
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

    interface EntityIgnore: Entity<EntityIgnore> {
        companion object : Entity.Factory<EntityIgnore>()
        var name: String
        @get:JsonIgnore
        var getIgnore: String
        @set:JsonIgnore
        var setIgnore: String
        @set:JsonProperty(access = JsonProperty.Access.READ_ONLY)
        var setReadOnly: String
        @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
        var getReadOnly: String
        @set:JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        var setWriteOnly: String
        @get:JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        var getWriteOnly: String
    }
    class JacksonDefaultIgnore {
        var name: String = ""
        @get:JsonIgnore
        var getIgnore: String = ""
        @set:JsonIgnore
        var setIgnore: String = ""
        @set:JsonProperty(access = JsonProperty.Access.READ_ONLY)
        var setReadOnly: String = ""
        @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
        var getReadOnly: String = ""
        @set:JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        var setWriteOnly: String = ""
        @get:JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        var getWriteOnly: String = ""
        override fun toString(): String {
            return "JacksonDefaultIgnore(" +
                    "name='$name', " +
                    "getIgnore='$getIgnore', " +
                    "setIgnore='$setIgnore', " +
                    "getReadOnly='$getReadOnly', " +
                    "setReadOnly='$setReadOnly', " +
                    "getWriteOnly='$getWriteOnly'" +
                    "setWriteOnly='$setWriteOnly'" +
                    ")"
        }
    }

    /**
     * make sure same behavior as jackson default serialize
     */
    @Test
    fun testJsonIgnoreSerialize() {
        val entityIgnore = EntityIgnore {
            name = "testIgnore"
            getIgnore = "getIgnore"
            setIgnore = "setIgnore"
            setReadOnly = "setReadOnly"
            getReadOnly = "getReadOnly"
            setWriteOnly = "setWriteOnly"
            getWriteOnly = "getWriteOnly"
        }
        val jacksonIgnore = JacksonDefaultIgnore()
        jacksonIgnore.name = "testIgnore"
        jacksonIgnore.getIgnore = "getIgnore"
        jacksonIgnore.setIgnore = "setIgnore"
        jacksonIgnore.setReadOnly = "setReadOnly"
        jacksonIgnore.getReadOnly = "getReadOnly"
        jacksonIgnore.setWriteOnly = "setWriteOnly"
        jacksonIgnore.getWriteOnly = "getWriteOnly"

        println(objectMapper.writeValueAsString(jacksonIgnore))
        println(objectMapper.writeValueAsString(entityIgnore))
        val entityMap = objectMapper.convertValue(entityIgnore, Map::class.java)
        val jacksonMap = objectMapper.convertValue(jacksonIgnore, Map::class.java)
        assert(entityMap == jacksonMap)

    }

    /**
     * make sure same behavior as jackson default deserialize
     */
    @Test
    fun testJsonIgnoreDeserialize() {
        val json = """
            {
              "name" : "testIgnore#2",
              "getIgnore" : "getIgnore#2",
              "setIgnore" : "setIgnore#2",
              "setReadOnly" : "setReadOnly#2",
              "getReadOnly" : "getReadOnly#2",
              "setWriteOnly" : "setWriteOnly#2",
              "getWriteOnly" : "getWriteOnly#2"
            }
        """
        val entityIgnore = objectMapper.readValue<EntityIgnore>(json)
        val jacksonIgnore = objectMapper.readValue<JacksonDefaultIgnore>(json)
        println(entityIgnore)
        println(jacksonIgnore)
        assert(entityIgnore.name == jacksonIgnore.name) // "testIgnore#2"
        assert(entityIgnore.getIgnore == jacksonIgnore.getIgnore) // ""
        assert(entityIgnore.setIgnore == jacksonIgnore.setIgnore) // ""
        assert(entityIgnore.setReadOnly == jacksonIgnore.setReadOnly) // ""
        assert(entityIgnore.getReadOnly == jacksonIgnore.getReadOnly) // ""
        assert(entityIgnore.setWriteOnly == jacksonIgnore.setWriteOnly) // "setWriteOnly#2"
        assert(entityIgnore.getWriteOnly == jacksonIgnore.getWriteOnly) // "getWriteOnly#2"
    }



    interface EntityPropertyIgnore: Entity<EntityPropertyIgnore> {
        companion object : Entity.Factory<EntityPropertyIgnore>()
        var name: String
        @set:JsonProperty(access = JsonProperty.Access.READ_ONLY)
        @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
        var setReadOnlyGetReadOnly: String
        @set:JsonProperty(access = JsonProperty.Access.READ_ONLY)
        @get:JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        var setReadOnlyGetWriteOnly: String
        @set:JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
        var setWriteOnlyGetReadOnly: String
        @set:JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @get:JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        var setWriteOnlyGetWriteOnly: String
    }

    class JacksonPropertyIgnore {
        var name = ""
        @set:JsonProperty(access = JsonProperty.Access.READ_ONLY)
        @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
        var setReadOnlyGetReadOnly = ""
        @set:JsonProperty(access = JsonProperty.Access.READ_ONLY)
        @get:JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        var setReadOnlyGetWriteOnly = ""
        @set:JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
        var setWriteOnlyGetReadOnly = ""
        @set:JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @get:JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        var setWriteOnlyGetWriteOnly = ""
        override fun toString(): String {
            return "JacksonPropertyIgnore(name='$name', setReadOnlyGetReadOnly='$setReadOnlyGetReadOnly', setReadOnlyGetWriteOnly='$setReadOnlyGetWriteOnly', setWriteOnlyGetReadOnly='$setWriteOnlyGetReadOnly', setWriteOnlyGetWriteOnly='$setWriteOnlyGetWriteOnly')"
        }
    }

    /**
     * [multi annotation]
     * make sure [JsonProperty]'s access behavior same as jackson default when serialize
     *
     * [JsonProperty]'s access on `get` takes precedence over `set` when serialize
     */
    @Test
    fun testPropertyIgnoreSerialize() {
        val jacksonIgnore = JacksonPropertyIgnore()
        jacksonIgnore.name = "propertyIgnore"
        jacksonIgnore.setReadOnlyGetReadOnly = "#1"
        jacksonIgnore.setReadOnlyGetWriteOnly = "#2" // no output , shows that `get` takes precedence over `set`
        jacksonIgnore.setWriteOnlyGetReadOnly = "#3" //  output , shows that `get` takes precedence over `set`
        jacksonIgnore.setWriteOnlyGetWriteOnly = "#4" // no output
        println(objectMapper.writeValueAsString(jacksonIgnore))
        val entityIgnore = EntityPropertyIgnore {
            name = "propertyIgnore"
            setReadOnlyGetReadOnly = "#1"
            setReadOnlyGetWriteOnly = "#2"
            setWriteOnlyGetReadOnly = "#3"
            setWriteOnlyGetWriteOnly = "#4"
        }
        println(objectMapper.writeValueAsString(entityIgnore))
        val entityMap = objectMapper.convertValue(entityIgnore, Map::class.java)
        val jacksonMap = objectMapper.convertValue(jacksonIgnore, Map::class.java)
        assert(entityMap == jacksonMap) // make sure same behavior as jackson default
    }

    /**
     * [multi annotation]
     * make sure [JsonProperty]'s access behavior same as jackson default when deserialize
     *
     * [JsonProperty]'s access on `set` takes precedence over `get`
     */
    @Test
    fun testPropertyIgnoreDeserialize() {
        val json = """
            {
              "name" : "propertyIgnore",
              "setReadOnlyGetReadOnly" : "#1",
              "setReadOnlyGetWriteOnly" : "#2",
              "setWriteOnlyGetReadOnly" : "#3",
              "setWriteOnlyGetWriteOnly" : "#4"
            }
        """
        val entityIgnore = objectMapper.readValue<EntityPropertyIgnore>(json)
        println(entityIgnore)

        val jacksonIgnore = objectMapper.readValue<JacksonPropertyIgnore>(json)
        println(jacksonIgnore)   // #3 & #4 deserialize into jacksonIgnore, shows that `set` takes precedence over `get`

        assert(entityIgnore.name == jacksonIgnore.name)
        assert(entityIgnore.setReadOnlyGetReadOnly == jacksonIgnore.setReadOnlyGetReadOnly)
        assert(entityIgnore.setReadOnlyGetWriteOnly == jacksonIgnore.setReadOnlyGetWriteOnly)
        assert(entityIgnore.setWriteOnlyGetReadOnly == jacksonIgnore.setWriteOnlyGetReadOnly)
        assert(entityIgnore.setWriteOnlyGetWriteOnly == jacksonIgnore.setWriteOnlyGetWriteOnly)
    }

    interface EntityAlias: Entity<EntityAlias> {
        companion object: Entity.Factory<EntityAlias>()
        var name :String
        @get:JsonAlias("aliasGetOnly")
        var getOnlyAlias: String
        @get:JsonProperty("propertyAliasGetOnly")
        var getOnlyPropertyAlias: String

        @set:JsonAlias("setAlias")
        @get:JsonAlias("getAlias")
        var setGetAlias: String
        @set:JsonProperty("propertySetAlias")
        @get:JsonProperty("propertyGetAlias")
        var setGetPropertyAlias: String

        // check priority of `JsonAlias` and `JsonProperty` when deserialize
        @set:JsonAlias("withAliasOnly")
        @set:JsonProperty("withPropertyOnly")
        var bothAliasAndProperty: String
    }

    class JacksonAlias {
        var name :String = ""
        @get:JsonAlias("aliasGetOnly")
        var getOnlyAlias = ""
        @get:JsonProperty("propertyAliasGetOnly")
        var getOnlyPropertyAlias = ""

        @set:JsonAlias("setAlias")
        @get:JsonAlias("getAlias")
        var setGetAlias: String = ""
        @set:JsonProperty("propertySetAlias")
        @get:JsonProperty("propertyGetAlias")
        var setGetPropertyAlias: String = ""

        // check priority of `JsonAlias` and `JsonProperty` when deserialize
        @set:JsonAlias("withAliasOnly")
        @set:JsonProperty("withPropertyOnly")
        var bothAliasAndProperty: String = ""
        override fun toString(): String {
            return "JacksonAlias(name='$name', " +
                    "setGetAlias='$setGetAlias', " +
                    "setGetPropertyAlias='$setGetPropertyAlias', " +
                    "bothAliasAndProperty='$bothAliasAndProperty')"
        }
    }


    /**
     * [multi annotation]
     * make sure json alias behavior same as jackson default when serialize
     *
     * [JsonAlias] don't work when serializing
     * [JsonProperty] on get takes precedence over set when serializing
     */
    @Test
    fun testAliasSerialize() {
        val entityAlias = EntityAlias {
            name = "alias"
            getOnlyAlias = "getOnlyAlias"
            getOnlyPropertyAlias = "getOnlyPropertyAlias"
            setGetAlias = "setGetAlias"
            setGetPropertyAlias = "setGetPropertyAlias"
            bothAliasAndProperty = "bothAliasAndProperty"
        }
        println(objectMapper.writeValueAsString(entityAlias))

        val jacksonAlias = JacksonAlias()
        jacksonAlias.name = "alias"
        jacksonAlias.getOnlyAlias = "getOnlyAlias"
        jacksonAlias.getOnlyPropertyAlias = "getOnlyPropertyAlias"
        jacksonAlias.setGetAlias = "setGetAlias"
        jacksonAlias.setGetPropertyAlias = "setGetPropertyAlias"
        jacksonAlias.bothAliasAndProperty = "bothAliasAndProperty"
        println(objectMapper.writeValueAsString(jacksonAlias))
//        {
//            "name" : "alias",
//            "setGetAlias" : "setGetAlias",  // key is `setGetAlias`, shows that `JsonAlias` don't work
//            "propertyGetAlias" : "setGetPropertyAlias" // key is `propertyGetAlias`, get takes precedence over set
//        }

        val entityMap = objectMapper.convertValue(entityAlias, Map::class.java)
        val jacksonMap = objectMapper.convertValue(jacksonAlias, Map::class.java)
        assert(entityMap == jacksonMap)
    }

    /**
     *  make sure json alias behavior same as jackson default when deserialize
     */
    @Test
    fun testDeserializeAliasName() {
        val json = """
            {
              "name" : "alias",
              "setAlias" : "setAlias",
              "propertySetAlias" : "propertySetAlias",
              "aliasGetOnly":"aliasGetOnly",
              "propertyAliasGetOnly":"propertyAliasGetOnly"
            }
        """
        val entityAlias = objectMapper.readValue<EntityAlias>(json)
        println(entityAlias)
        val jacksonAlias = objectMapper.readValue<JacksonAlias>(json)
        println(jacksonAlias)
        assert(entityAlias.name == jacksonAlias.name)
        assert(entityAlias.setGetAlias == jacksonAlias.setGetAlias) // value: setAlias
        assert(entityAlias.setGetPropertyAlias == jacksonAlias.setGetPropertyAlias) // value: propertySetAlias

        // `JsonAlias` on `get` works
        assert(jacksonAlias.getOnlyAlias == "aliasGetOnly")
        assert(entityAlias.getOnlyAlias == jacksonAlias.getOnlyAlias)

        // `JsonProperty` on `get` works
        assert(jacksonAlias.getOnlyPropertyAlias == "propertyAliasGetOnly")
        assert(entityAlias.getOnlyPropertyAlias == jacksonAlias.getOnlyPropertyAlias)
    }

    /**
     *  make sure json alias behavior same as jackson default when deserialize
     *
     *  prop with [JsonAlias], alias name contains prop's origin strategy name but not origin name
     *  prop with [JsonProperty], alias name don't contains prop's origin strategy name or origin name
     *
     *  prop with both [JsonAlias] and [JsonProperty], alias name don't contains prop's origin strategy name
     */
    @Test
    fun testDeserializeAliasName2() {
        val mapper = ObjectMapper().findAndRegisterModules()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .setPropertyNamingStrategy(PropertyNamingStrategies.SnakeCaseStrategy())
        val json = """
            {
              "name" : "alias",
              "set_get_alias" : "set_get_alias",
              "setGetAlias" : "setGetAlias",
              
              "setGetPropertyAlias" : "setGetPropertyAlias",
              "set_get_property_alias" : "set_get_property_alias",
              
              "withPropertyOnly":"withPropertyOnly",
              "withAliasOnly":"withAliasOnly",
              "both_alias_and_property": "both_alias_and_property" 
            }
        """
        val entityAlias = mapper.readValue<EntityAlias>(json)
        println(entityAlias)
        val jacksonAlias = mapper.readValue<JacksonAlias>(json)
        println(jacksonAlias)

        // we use prop's origin name and origin strategy name to deserialize,
        // according to order issue in `default jackson deserialize#2`
        // shows that prop with `JsonAlias`, its alias name contains prop's origin strategy name but not origin name
        assert(jacksonAlias.setGetAlias == "set_get_alias")
        assert(entityAlias.setGetAlias == jacksonAlias.setGetAlias)

        // prop with `JsonProperty`, its alias name don't contains either prop's origin strategy name or origin name
        assert(jacksonAlias.setGetPropertyAlias.isEmpty())
        assert(entityAlias.setGetPropertyAlias.isEmpty())


        // "withAliasOnly", `JsonProperty` and `JsonAlias` has same priority
        // but prop with `JsonProperty`,it don't contains either prop's origin strategy name
        assert(jacksonAlias.bothAliasAndProperty == "withAliasOnly")
        assert( entityAlias.bothAliasAndProperty == jacksonAlias.bothAliasAndProperty)
    }


    /**
     *  [multi annotation]
     *  make sure json alias behavior same when deserialize
     *
     *  [JsonAlias] on `set` takes precedence over `get`
     *  [JsonProperty] on `set` takes precedence over `get`
     */
    @Test
    fun testAliasDeserializeAliasPropertyPriority() {
        val mapper = ObjectMapper().findAndRegisterModules().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        val json = """
            {
              "name" : "alias",
              "setAlias" : "setAlias",
              "getAlias" : "getAlias",
              "propertySetAlias" : "propertySetAlias",
              "propertyGetAlias" : "propertyGetAlias"
            }
        """
        val entityAlias = mapper.readValue<EntityAlias>(json)
        println(entityAlias)

        val jacksonAlias = mapper.readValue<JacksonAlias>(json)
        println(jacksonAlias)

        // according to order issue in defaultJacksonDeserialize2
        // setGetAlias's value is 'setAlias' shows that `JsonAlias` on `set` takes precedence over `get`
        assert(jacksonAlias.setGetAlias == "setAlias")
        assert(entityAlias.setGetAlias == jacksonAlias.setGetAlias)

        // JsonProperties on `set` takes precedence over `get`
        assert(jacksonAlias.setGetPropertyAlias == "propertySetAlias")
        assert(entityAlias.setGetPropertyAlias == jacksonAlias.setGetPropertyAlias)
    }

    /**
     * this test and the next #2 shows that [JsonAlias] and [JsonProperty] has same priority when deserialize
     */
    @Test
    fun testAliasAndPropertyDeserializePriority()  {
        val json = """
            {
              "withPropertyOnly":"withPropertyOnly",
              "withAliasOnly":"withAliasOnly"
            }
        """
        val entityAlias = objectMapper.readValue<EntityAlias>(json)
        println(entityAlias)
        val jacksonAlias = objectMapper.readValue<JacksonAlias>(json)
        println(jacksonAlias)
        assert(jacksonAlias.bothAliasAndProperty == "withAliasOnly") // take last value to inject
        assert(entityAlias.bothAliasAndProperty == jacksonAlias.bothAliasAndProperty)
    }

    @Test
    fun testAliasAndPropertyDeserializePriority2()  {
        val json = """
            {
              "withAliasOnly":"withAliasOnly",
              "withPropertyOnly":"withPropertyOnly"
            }
        """
        val entityAlias = objectMapper.readValue<EntityAlias>(json)
        println(entityAlias)
        val jacksonAlias = objectMapper.readValue<JacksonAlias>(json)
        println(jacksonAlias)
        assert(jacksonAlias.bothAliasAndProperty == "withPropertyOnly") // take last value to inject
        assert(entityAlias.bothAliasAndProperty == jacksonAlias.bothAliasAndProperty)
    }

}