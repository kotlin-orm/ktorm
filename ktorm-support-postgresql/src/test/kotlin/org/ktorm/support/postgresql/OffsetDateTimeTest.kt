package org.ktorm.support.postgresql

import org.junit.Test
import org.ktorm.dsl.*
import org.ktorm.schema.Table
import org.ktorm.schema.int
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class OffsetDateTimeTest : BasePostgreSqlTest() {

  object OffsetDatetimes : Table<Nothing>("t_offset_datetime") {
    val id = int("id").primaryKey()
    val createdAt = datetimeOffset("created_at")
  }

  @Test
  fun test() {
    val value = OffsetDateTime.now()
    database.insert(OffsetDatetimes) {
      set(OffsetDatetimes.createdAt, value)
    }
    val results = database
      .from(OffsetDatetimes)
      .select(OffsetDatetimes.createdAt)
      .where(OffsetDatetimes.id eq 1)
      .map { row ->
        row[OffsetDatetimes.createdAt]
      }
    val offsetDateTime = results[0]
    assertNotNull(offsetDateTime)
    assertEquals(value.year, offsetDateTime.year)
    assertEquals(value.month, offsetDateTime.month)
    assertEquals(value.dayOfMonth, offsetDateTime.dayOfMonth)
    assertEquals(value.hour, offsetDateTime.hour)
    assertEquals(value.minute, offsetDateTime.minute)
    assertEquals(value.second, offsetDateTime.second)
    assertEquals(value.offset, offsetDateTime.offset)
  }

}
