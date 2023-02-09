package org.ktorm.support.postgresql

import org.junit.Test
import org.ktorm.dsl.*
import kotlin.test.assertEquals

class FunctionsTest : BasePostgreSqlTest() {
    @Test
    fun testArrayPositionEnumCollection() {
        database.insert(TableWithEnum) {
            set(it.current_mood, Mood.SAD)
        }
        database.insert(TableWithEnum) {
            set(it.current_mood, Mood.HAPPY)
        }

        val moodsSorted = database
            .from(TableWithEnum)
            .select()
            .orderBy(arrayPosition(listOf(Mood.SAD, Mood.HAPPY), TableWithEnum.current_mood).asc())
            .map { row ->
                row[TableWithEnum.current_mood]
            }

        assertEquals(listOf(Mood.SAD, Mood.HAPPY, Mood.HAPPY), moodsSorted)
    }

    @Test
    fun testArrayPositionEnumArray() {
        database.insert(TableWithEnum) {
            set(it.current_mood, Mood.SAD)
        }
        database.insert(TableWithEnum) {
            set(it.current_mood, Mood.HAPPY)
        }

        val moodsSorted = database
            .from(TableWithEnum)
            .select()
            .orderBy(arrayPosition(arrayOf(Mood.SAD, Mood.HAPPY), TableWithEnum.current_mood).asc())
            .map { row ->
                row[TableWithEnum.current_mood]
            }

        assertEquals(listOf(Mood.SAD, Mood.HAPPY, Mood.HAPPY), moodsSorted)
    }

    @Test
    fun testArrayPositionTextArray() {
        val namesSorted = database
            .from(Employees)
            .select()
            .orderBy(arrayPosition(arrayOf("tom", "vince", "marry"), Employees.name).asc())
            .map { row ->
                row[Employees.name]
            }

        assertEquals(listOf("tom", "vince", "marry", "penny"), namesSorted)
    }
}
