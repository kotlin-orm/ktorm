package org.ktorm.autotable

import org.junit.Test
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import java.math.BigDecimal
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.time.*
import java.util.*
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.jvmName

class TypeAdapterFactoryTest {
    class TestType(
        val testField: TestType? = null,
        val testBigDecimal: BigDecimal? = null,
        val testBoolean: Boolean? = null,
        val testBytes: ByteArray? = null,
        val testDate: Date? = null,
        val testDouble: Double? = null,
        val testEnum: Enum<*>? = null,
        val testFloat: Float? = null,
        val testInstant: Instant? = null,
        val testInt: Int? = null,
        val testLocalDate: LocalDate? = null,
        val testLocalDateTime: LocalDateTime? = null,
        val testLocalTime: LocalTime? = null,
        val testLong: Long? = null,
        val testMonth: Month? = null,
        val testString: String? = null,
        val testTime: Time? = null,
        val testTimestamp: Timestamp? = null,
        val testUUID: UUID? = null,
        val testYear: Year? = null,
        val testYearMonth: YearMonth? = null,
    )

    object TestTypeAdapter : TypeAdapter<String> {
        override fun register(table: BaseTable<Any>, field: KProperty1<Any, String>): Column<String>? {
            return if (field.returnType.jvmErasure == TestType::class) {
                table.text(field)
            } else {
                null
            }
        }
    }

    @Test
    fun testScanPackage() {
        TypeAdapterFactory.scanPackage(TypeAdapterFactoryTest::class.jvmName)
        AutoTable<TestType>().rebuild()
        // get testField column with registered TestTypeAdapter
        TestType::testField.column
    }

    @Test
    fun testRegisterAdapter() {
        TypeAdapterFactory.registerAdapter(TestTypeAdapter)
        AutoTable<TestType>().rebuild()
        // get testField column with registered TestTypeAdapter
        TestType::testField.column
    }

    @Test
    fun testBigDecimal() {
        TestType::testBigDecimal.column
    }

    @Test
    fun testBoolean() {
        TestType::testBoolean.column
    }

    @Test
    fun testBytes() {
        TestType::testBytes.column
    }

    @Test
    fun testDate() {
        TestType::testDate.column
    }

    @Test
    fun testDouble() {
        TestType::testDouble.column
    }

    @Test
    fun testEnum() {
        TestType::testEnum.column
    }

    @Test
    fun testFloat() {
        TestType::testFloat.column
    }

    @Test
    fun testInstant() {
        TestType::testInstant.column
    }

    @Test
    fun testInt() {
        TestType::testInt.column
    }

    @Test
    fun testLocalDate() {
        TestType::testLocalDate.column
    }

    @Test
    fun testLocalDateTime() {
        TestType::testLocalDateTime.column
    }

    @Test
    fun testLocalTime() {
        TestType::testLocalTime.column
    }

    @Test
    fun testLong() {
        TestType::testLong.column
    }

    @Test
    fun testMonth() {
        TestType::testMonth.column
    }

    @Test
    fun testString() {
        TestType::testString.column
    }

    @Test
    fun testTime() {
        TestType::testTime.column
    }

    @Test
    fun testTimestamp() {
        TestType::testTimestamp.column
    }

    @Test
    fun testUUID() {
        TestType::testUUID.column
    }

    @Test
    fun testYear() {
        TestType::testYear.column
    }

    @Test
    fun testYearMonth() {
        TestType::testYearMonth.column
    }
}