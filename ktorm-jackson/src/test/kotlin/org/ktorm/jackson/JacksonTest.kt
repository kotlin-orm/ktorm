package org.ktorm.jackson

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.junit.Test
import org.ktorm.entity.Entity
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Created by vince on Dec 09, 2018.
 */
class JacksonTest {

    private val objectMapper = ObjectMapper()
        .configure(SerializationFeature.INDENT_OUTPUT, true)
        .findAndRegisterModules()

    private val typedObjectMapper = ObjectMapper().apply {
        activateDefaultTyping(polymorphicTypeValidator, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)
        configure(SerializationFeature.INDENT_OUTPUT, true)
        findAndRegisterModules()
    }

    private val separator = "$"

    private val foo = Foo {
        boolean = true
        byte = 1
        short = 2
        int = 3
        long = 4
        ulong = 123U
        ulong0 = 1230U
        float = 5.0F
        double = 6.0
        bigInteger = BigInteger("7")
        bigDecimal = BigDecimal("8")
        string = "9"
        byteArray = byteArrayOf(10)
        booleanArray = booleanArrayOf(false, true)
        shortArray = shortArrayOf(11)
        intArray = intArrayOf(12)
        longArray = longArrayOf(13)
        floatArray = floatArrayOf(14.0F)
        doubleArray = doubleArrayOf(15.0)
        stringArray = arrayOf("16")
        arrayList = arrayListOf("17")
        intList = listOf(16, 17)
        longList = listOf(17, 18)
        nullList = null
        set = setOf("18")
        list = listOf(
            Department {
                id = 19
                name = "20"
            }
        )
        collection = setOf("21")
        entity = Department {
            id = 22
            name = "23"
        }
        nestedIntArray = arrayOf(
            intArrayOf(27, 28),
            intArrayOf(29, 30)
        )
        nestedArray = arrayOf(
            arrayOf(
                Department {
                    id = 31
                    name = "32"
                }
            )
        )
        nestedCollection = listOf(
            listOf(
                Department {
                    id = 33
                    name = "34"
                }
            )
        )
        map = mapOf(
            36L to Department {
                id = 35
                name = "36"
            }
        )
    }

    private fun checkFoo(f: Foo) {
        assert(f.boolean == foo.boolean)
        assert(f.byte == foo.byte)
        assert(f.short == foo.short)
        assert(f.int == foo.int)
        assert(f.long == foo.long)
        assert(f.ulong == foo.ulong)
        assert(f.ulong0 == foo.ulong0)
        assert(f.float == foo.float)
        assert(f.double == foo.double)
        assert(f.bigInteger == foo.bigInteger)
        assert(f.bigDecimal == foo.bigDecimal)
        assert(f.string == foo.string)
        assert(f.byteArray.contentEquals(foo.byteArray))
        assert(f.booleanArray.contentEquals(foo.booleanArray))
        assert(f.shortArray.contentEquals(foo.shortArray))
        assert(f.intArray.contentEquals(foo.intArray))
        assert(f.longArray.contentEquals(foo.longArray))
        assert(f.floatArray.contentEquals(foo.floatArray))
        assert(f.doubleArray.contentEquals(foo.doubleArray))
        assert(f.stringArray.contentEquals(foo.stringArray))
        assert(f.arrayList == foo.arrayList)
        assert(f.intList == foo.intList)
        assert(f.longList == foo.longList)
        assert(f.nullList == foo.nullList)
        assert(f.set == foo.set)
        assert(f.list[0].contentEquals(foo.list[0]))
        assert(f.collection.first() == foo.collection.first())
        assert(f.entity.contentEquals(foo.entity))
        assert(f.nestedIntArray.contentDeepEquals(foo.nestedIntArray))
        assert(f.nestedArray[0][0].contentEquals(foo.nestedArray[0][0]))
        assert(f.nestedCollection.first().first().contentEquals(foo.nestedCollection.first().first()))
        assert(f.map.keys == foo.map.keys)
        assert(f.map.values.first().contentEquals(foo.map.values.first()))
    }

    @Test
    fun testToJson() {
        println(objectMapper.writeValueAsString(foo))
    }

    @Test
    fun testParseJson() {
        val json = """
            {
              "boolean" : true,
              "byte" : 1,
              "short" : 2,
              "int" : 3,
              "long" : 4,
              "ulong" : 123,
              "ulong0" : 1230,
              "float" : 5.0,
              "double" : 6.0,
              "bigInteger" : 7,
              "bigDecimal" : 8,
              "string" : "9",
              "byteArray" : "Cg==",
              "booleanArray" : [ false, true ],
              "shortArray" : [ 11 ],
              "intArray" : [ 12 ],
              "longArray" : [ 13 ],
              "floatArray" : [ 14.0 ],
              "doubleArray" : [ 15.0 ],
              "stringArray" : [ "16" ],
              "arrayList" : [ "17" ],
              "intList" : [ 16, 17 ],
              "longList" : [ 17, 18 ],
              "nullList" : null,
              "set" : [ "18" ],
              "list" : [ {
                "id" : 19,
                "name" : "20"
              } ],
              "collection" : [ "21" ],
              "entity" : {
                "id" : 22,
                "name" : "23"
              },
              "nestedIntArray" : [ [ 27, 28 ], [ 29, 30 ] ],
              "nestedArray" : [ [ {
                "id" : 31,
                "name" : "32"
              } ] ],
              "nestedCollection" : [ [ {
                "id" : 33,
                "name" : "34"
              } ] ],
              "map" : {
                "36" : {
                  "id" : 35,
                  "name" : "36"
                }
              }
            }
        """

        val foo = objectMapper.readValue(json, Foo::class.java)
        checkFoo(foo)
    }

    @Test
    fun testToTypedJson() {
        println(typedObjectMapper.writeValueAsString(foo))
    }

    @Test
    fun testParseTypedJson() {
        val json = """
            {
              "@class" : "org.ktorm.jackson.JacksonTest${separator}Foo",
              "boolean" : true,
              "byte" : 1,
              "short" : 2,
              "int" : 3,
              "long" : 4,
              "ulong" : 123,
              "ulong0" : 1230,
              "float" : 5.0,
              "double" : 6.0,
              "bigInteger" : [ "java.math.BigInteger", 7 ],
              "bigDecimal" : [ "java.math.BigDecimal", 8 ],
              "string" : "9",
              "byteArray" : "Cg==",
              "booleanArray" : [ false, true ],
              "shortArray" : [ 11 ],
              "intArray" : [ 12 ],
              "longArray" : [ 13 ],
              "floatArray" : [ 14.0 ],
              "doubleArray" : [ 15.0 ],
              "stringArray" : [ "16" ],
              "arrayList" : [ "java.util.ArrayList", [ "17" ] ],
              "intList" : [ "java.util.Arrays${separator}ArrayList", [ 16, 17 ] ],
              "longList" : [ "java.util.Arrays${separator}ArrayList", [ 17, 18 ] ],
              "nullList" : null,
              "set" : [ "java.util.Collections${separator}SingletonSet", [ "18" ] ],
              "list" : [ "java.util.Collections${separator}SingletonList", [ {
                "@class" : "org.ktorm.jackson.JacksonTest${separator}Department",
                "id" : 19,
                "name" : "20"
              } ] ],
              "collection" : [ "java.util.Collections${separator}SingletonSet", [ "21" ] ],
              "entity" : {
                "@class" : "org.ktorm.jackson.JacksonTest${separator}Department",
                "id" : 22,
                "name" : "23"
              },
              "nestedIntArray" : [ [ 27, 28 ], [ 29, 30 ] ],
              "nestedArray" : [ "[[Lorg.ktorm.jackson.JacksonTest${separator}Department;", [ [ "[Lorg.ktorm.jackson.JacksonTest${separator}Department;", [ {
                "@class" : "org.ktorm.jackson.JacksonTest${separator}Department",
                "id" : 31,
                "name" : "32"
              } ] ] ] ],
              "nestedCollection" : [ "java.util.Collections${separator}SingletonList", [ [ "java.util.Collections${separator}SingletonList", [ {
                "@class" : "org.ktorm.jackson.JacksonTest${separator}Department",
                "id" : 33,
                "name" : "34"
              } ] ] ] ],
              "map" : {
                "@class" : "java.util.Collections${separator}SingletonMap",
                "36" : {
                  "@class" : "org.ktorm.jackson.JacksonTest${separator}Department",
                  "id" : 35,
                  "name" : "36"
                }
              }
            }
        """

        val foo = typedObjectMapper.readValue(json, Foo::class.java)
        checkFoo(foo)
    }

    @Test
    fun testNestedEntityToJson() {
        val bar = Bar(
            foo = Foo {
                int = 123
            }
        )

        println(objectMapper.writeValueAsString(bar))
    }

    @Test
    fun testParseNestedEntity() {
        val json = """
            {
              "foo" : {
                "int" : 123
              }
            }
        """

        val bar = objectMapper.readValue(json, Bar::class.java)
        println(bar)
        assert(bar.foo.int == 123)
    }

    @Test
    fun testNestedEntityToTypedJson() {
        val bar = Bar(
            foo = Foo {
                int = 123
            }
        )

        println(typedObjectMapper.writeValueAsString(bar))
    }

    @Test
    fun testParsTypedNestedEntity() {
        val json = """
            {
              "foo" : {
                "@class" : "org.ktorm.jackson.JacksonTest${separator}Foo",
                "int" : 123
              }
            }
        """

        val bar = typedObjectMapper.readValue(json, Bar::class.java)
        println(bar)
        assert(bar.foo.int == 123)
    }

    @Test
    fun testEmptyObject() {
        val foo = Foo()
        val json = typedObjectMapper.writeValueAsString(foo)
        println(json)
        println(typedObjectMapper.readValue(json, Foo::class.java))
    }

    interface Department : Entity<Department> {
        companion object : Entity.Factory<Department>()
        var id: Int
        var name: String
        var location: String

        fun contentEquals(other: Department): Boolean {
            return id == other.id && name == other.name && location == other.location
        }
    }

    interface Foo : Entity<Foo> {
        companion object : Entity.Factory<Foo>()
        var boolean: Boolean
        var byte: Byte
        var short: Short
        var int: Int
        var long: Long
        var ulong: ULong
        var ulong0: ULong?
        var float: Float
        var double: Double
        var bigInteger: BigInteger
        var bigDecimal: BigDecimal
        var string: String
        var byteArray: ByteArray
        var booleanArray: BooleanArray
        var shortArray: ShortArray
        var intArray: IntArray
        var longArray: LongArray
        var floatArray: FloatArray
        var doubleArray: DoubleArray
        var stringArray: Array<String>
        var arrayList: ArrayList<String>
        var intList: List<Int>
        var longList: List<Long>
        var nullList: List<Long>?
        var set: Set<String>
        var list: List<Department>
        var collection: Collection<String>
        var entity: Department
        var nestedIntArray: Array<IntArray>
        var nestedArray: Array<Array<Department>>
        var nestedCollection: Collection<Collection<Department>>
        var map: Map<Long, Department>
    }

    data class Bar(val foo: Foo)
}