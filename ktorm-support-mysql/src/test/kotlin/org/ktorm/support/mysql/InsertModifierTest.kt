package org.ktorm.support.mysql

import org.junit.Test
import org.ktorm.entity.Entity
import org.ktorm.entity.count
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar

class InsertModifierTest : BaseMySqlTest() {

    object Teams : Table<Team>("t_team") {
        val id = int("id").primaryKey().bindTo { it.id }
        val name = varchar("name").bindTo { it.name }
    }

    interface Team : Entity<Team> {
        var id: Int
        var name: String
    }

    @Test
    fun testInsertIgnore() {
        assert(database.insertIgnore(Teams) {
            set(it.name, "Lakers")
        } > 0)
        assert(database.sequenceOf(Teams).count() == 1)
        assert(database.insertIgnore(Teams) {
            set(it.name, "Lakers")
        } == 0)
        assert(database.sequenceOf(Teams).count() == 1)
    }
}