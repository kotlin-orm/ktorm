package org.ktorm.support.mysql

import org.junit.Test
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.dsl.inList
import org.ktorm.dsl.insertAndGenerateKey
import org.ktorm.entity.*
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DefaultValueTest : BaseMySqlTest() {

    object Users : Table<User>("t_user") {
        val id = int("id").primaryKey().bindTo { it.id }
        val username = varchar("username").bindTo { it.username }
        val age = int("age").bindTo { it.age }
    }

    interface User : Entity<User> {
        var id: Int
        var username: String
        var age: Int?
    }

    private val Database.users: EntitySequence<User, Users> get() = this.sequenceOf(Users)

    @Test
    fun insert() {
        val id = database.insertAndGenerateKey(Users) {
            set(it.id, it.id.defaultValue())
            set(it.username, it.username.defaultValue())
            set(it.age, it.age.defaultValue())
        }
        assertIs<Int>(id)
        val entity = database.users.firstOrNull { it.id eq id }
        assertNotNull(entity)
        assertNotNull(entity.id)
        assertEquals(entity.username, "default")
        assertNull(entity.age)
    }

    @Test
    fun bulkInsert() {
        database.bulkInsert(Users) {
            item {
                set(it.id, 10)
                set(it.username, it.username.defaultValue())
                set(it.age, 10)
            }
            item {
                set(it.id, 11)
                set(it.username, it.username.defaultValue())
                set(it.age, 11)
            }
        }
        val defaultValues = database.users.filter { it.id inList (10..11).toList() }.toList()
        assertEquals(defaultValues.size, 2)
        for (defaultValue in defaultValues) {
            assertEquals(defaultValue.username, "default")
        }

        database.bulkInsertOrUpdate(Users) {
            item {
                set(it.id, 10)
                set(it.age, 10)
            }
            onDuplicateKey {
                set(it.age, it.age.defaultValue())
            }
        }
        val user = database.users.first { it.id eq 10 }
        assertNull(user.age)
    }

}
