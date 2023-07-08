package org.ktorm.ksp.compiler.parser

import org.junit.Test
import org.ktorm.ksp.compiler.BaseKspTest

class SqlTypeTest : BaseKspTest() {

    @Test
    fun testDefaultSqlType() = runKotlin("""
        @Table
        data class User(
            val int: Int,
            val string: String,
            val boolean: Boolean,
            val long: Long,
            val short: Short,
            val double: Double,
            val float: Float,
            val bigDecimal: BigDecimal,
            val date: java.sql.Date,
            val time: Time,
            val timestamp: Timestamp,
            val localDateTime: LocalDateTime,
            val localDate: LocalDate,
            val localTime: LocalTime,
            val monthDay: MonthDay,
            val yearMonth: YearMonth,
            val year: Year,
            val instant: Instant,
            val uuid: UUID,
            val byteArray: ByteArray,
            val gender: Gender
        )
        
        enum class Gender {
            MALE,
            FEMALE
        }
        
        fun run() {
            assert(Users.int.sqlType == org.ktorm.schema.IntSqlType)
            assert(Users.string.sqlType == org.ktorm.schema.VarcharSqlType)
            assert(Users.boolean.sqlType == org.ktorm.schema.BooleanSqlType)
            assert(Users.long.sqlType == org.ktorm.schema.LongSqlType)
            assert(Users.short.sqlType == org.ktorm.schema.ShortSqlType)
            assert(Users.double.sqlType == org.ktorm.schema.DoubleSqlType)
            assert(Users.float.sqlType == org.ktorm.schema.FloatSqlType)
            assert(Users.bigDecimal.sqlType == org.ktorm.schema.DecimalSqlType)
            assert(Users.date.sqlType == org.ktorm.schema.DateSqlType)
            assert(Users.time.sqlType == org.ktorm.schema.TimeSqlType)
            assert(Users.timestamp.sqlType == org.ktorm.schema.TimestampSqlType)
            assert(Users.localDateTime.sqlType == org.ktorm.schema.LocalDateTimeSqlType)
            assert(Users.localDate.sqlType == org.ktorm.schema.LocalDateSqlType)
            assert(Users.localTime.sqlType == org.ktorm.schema.LocalTimeSqlType)
            assert(Users.monthDay.sqlType == org.ktorm.schema.MonthDaySqlType)
            assert(Users.yearMonth.sqlType == org.ktorm.schema.YearMonthSqlType)
            assert(Users.year.sqlType == org.ktorm.schema.YearSqlType)
            assert(Users.instant.sqlType == org.ktorm.schema.InstantSqlType)
            assert(Users.uuid.sqlType == org.ktorm.schema.UuidSqlType)
            assert(Users.byteArray.sqlType == org.ktorm.schema.BytesSqlType)
            assert(Users.gender.sqlType is org.ktorm.schema.EnumSqlType<*>)
        }
    """.trimIndent())

    @Test
    fun testCustomSqlType() = runKotlin("""
        @Table
        data class User(
            @PrimaryKey
            var id: Int,
            @Column(sqlType = UsernameSqlType::class)
            var username: Username,
            var age: Int,
            var gender: Int
        )
        
        data class Username(
            val firstName:String,
            val lastName:String
        )
        
        object UsernameSqlType : org.ktorm.schema.SqlType<Username>(Types.VARCHAR, "varchar") {
            override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Username) {
                ps.setString(index, parameter.firstName + "#" + parameter.lastName)
            }
        
            override fun doGetResult(rs: ResultSet, index: Int): Username? {
                val (firstName, lastName) = rs.getString(index)?.split("#") ?: return null
                return Username(firstName, lastName)
            }
        }
        
        fun run() {
            assert(Users.username.sqlType == UsernameSqlType)
            assert(Users.username.sqlType.typeCode == Types.VARCHAR)
            assert(Users.username.sqlType.typeName == "varchar")
            
            database.users.add(User(100, Username("Vincent", "Lau"), 28, 0))
            assert(database.users.first { it.id eq 100 } == User(100, Username("Vincent", "Lau"), 28, 0))
        }
    """.trimIndent())

    @Test
    fun testCustomSqlTypeWithConstructor() = runKotlin("""
        @Table
        data class User(
            @PrimaryKey
            var id: Int,
            var username: String,
            var age: Int,
            @Column(sqlType = IntEnumSqlType::class)
            var gender: Gender
        )
        
        enum class Gender {
            MALE,
            FEMALE
        }
        
        class IntEnumSqlType<E : Enum<E>>(val enumClass: Class<E>) : org.ktorm.schema.SqlType<E>(Types.INTEGER, "int") {
            @Suppress("UNCHECKED_CAST")
            constructor(typeRef: org.ktorm.schema.TypeReference<E>) : this(typeRef.referencedType as Class<E>)
        
            override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: E) {
                ps.setInt(index, parameter.ordinal)
            }
        
            override fun doGetResult(rs: ResultSet, index: Int): E? {
                return enumClass.enumConstants[rs.getInt(index)]
            }
        }
        
        fun run() {
            assert(Users.gender.sqlType::class.simpleName == "IntEnumSqlType")
            assert(Users.gender.sqlType.typeCode == Types.INTEGER)
            assert(Users.gender.sqlType.typeName == "int")
            
            database.users.add(User(100, "Vincent", 28, Gender.MALE))
            assert(database.users.first { it.id eq 100 } == User(100, "Vincent", 28, Gender.MALE))
        }
    """.trimIndent())
}
