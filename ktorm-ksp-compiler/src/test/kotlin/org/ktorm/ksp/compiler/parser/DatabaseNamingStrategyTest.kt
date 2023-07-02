package org.ktorm.ksp.compiler.parser

import org.junit.Test
import org.ktorm.ksp.compiler.BaseKspTest
import org.ktorm.ksp.compiler.util.CamelCase
import kotlin.test.assertEquals

class DatabaseNamingStrategyTest : BaseKspTest() {

    @Test
    fun testCamelCase() {
        assertEquals("abc_def", CamelCase.toLowerSnakeCase("abcDef"))
        assertEquals("abc_def", CamelCase.toLowerSnakeCase("AbcDef"))
        assertEquals("ABC_DEF", CamelCase.toUpperSnakeCase("abcDef"))
        assertEquals("ABC_DEF", CamelCase.toUpperSnakeCase("AbcDef"))
        assertEquals("abcDef", CamelCase.toFirstLowerCamelCase("abcDef"))
        assertEquals("abcDef", CamelCase.toFirstLowerCamelCase("AbcDef"))

        assertEquals("abc_def", CamelCase.toLowerSnakeCase("ABCDef"))
        assertEquals("ABC_DEF", CamelCase.toUpperSnakeCase("ABCDef"))
        assertEquals("abcDef", CamelCase.toFirstLowerCamelCase("ABCDef"))

        assertEquals("io_utils", CamelCase.toLowerSnakeCase("IOUtils"))
        assertEquals("IO_UTILS", CamelCase.toUpperSnakeCase("IOUtils"))
        assertEquals("ioUtils", CamelCase.toFirstLowerCamelCase("IOUtils"))

        assertEquals("pwd_utils", CamelCase.toLowerSnakeCase("PWDUtils"))
        assertEquals("PWD_UTILS", CamelCase.toUpperSnakeCase("PWDUtils"))
        assertEquals("pwdUtils", CamelCase.toFirstLowerCamelCase("PWDUtils"))

        assertEquals("pwd_utils", CamelCase.toLowerSnakeCase("PwdUtils"))
        assertEquals("PWD_UTILS", CamelCase.toUpperSnakeCase("PwdUtils"))
        assertEquals("pwdUtils", CamelCase.toFirstLowerCamelCase("PwdUtils"))

        assertEquals("test_io", CamelCase.toLowerSnakeCase("testIO"))
        assertEquals("TEST_IO", CamelCase.toUpperSnakeCase("testIO"))
        assertEquals("testIO", CamelCase.toFirstLowerCamelCase("testIO"))

        assertEquals("test_pwd", CamelCase.toLowerSnakeCase("testPWD"))
        assertEquals("TEST_PWD", CamelCase.toUpperSnakeCase("testPWD"))
        assertEquals("testPWD", CamelCase.toFirstLowerCamelCase("testPWD"))

        assertEquals("test_pwd", CamelCase.toLowerSnakeCase("testPwd"))
        assertEquals("TEST_PWD", CamelCase.toUpperSnakeCase("testPwd"))
        assertEquals("testPwd", CamelCase.toFirstLowerCamelCase("testPwd"))

        assertEquals("a2c_count", CamelCase.toLowerSnakeCase("A2CCount"))
        assertEquals("A2C_COUNT", CamelCase.toUpperSnakeCase("A2CCount"))
        assertEquals("a2cCount", CamelCase.toFirstLowerCamelCase("A2CCount"))
    }

    @Test
    fun testDefaultNaming() = runKotlin("""
        @Table
        interface UserProfile: Entity<UserProfile> {
            @PrimaryKey
            var id: Int
            var publicEmail: String
            var profilePicture: Int
            @References
            var company: Company
        }
        
        @Table
        interface Company: Entity<Company> {
            @PrimaryKey
            var id: Int
            var name: String
        }
        
        fun run() {
            assert(Companies.tableName == "company")
            assert(Companies.columns.map { it.name }.toSet() == setOf("id", "name"))
            assert(UserProfiles.tableName == "user_profile")
            assert(UserProfiles.columns.map { it.name }.toSet() == setOf("id", "public_email", "profile_picture", "company_id"))
        }
    """.trimIndent())

    @Test
    fun testUpperCamelCaseNamingByAlias() = runKotlin("""
        @Table
        interface UserProfile: Entity<UserProfile> {
            @PrimaryKey
            var id: Int
            var publicEmail: String
            var profilePicture: Int
            @References
            var company: Company
        }
        
        @Table
        interface Company: Entity<Company> {
            @PrimaryKey
            var id: Int
            var name: String
        }
        
        fun run() {
            assert(Companies.tableName == "COMPANY")
            assert(Companies.columns.map { it.name }.toSet() == setOf("ID", "NAME"))
            assert(UserProfiles.tableName == "USER_PROFILE")
            assert(UserProfiles.columns.map { it.name }.toSet() == setOf("ID", "PUBLIC_EMAIL", "PROFILE_PICTURE", "COMPANY_ID"))
        }
    """.trimIndent(), "ktorm.dbNamingStrategy" to "upper-snake-case")

    @Test
    fun testUpperCamelCaseNamingByClassName() = runKotlin("""
        @Table
        interface UserProfile: Entity<UserProfile> {
            @PrimaryKey
            var id: Int
            var publicEmail: String
            var profilePicture: Int
            @References
            var company: Company
        }
        
        @Table
        interface Company: Entity<Company> {
            @PrimaryKey
            var id: Int
            var name: String
        }
        
        fun run() {
            assert(Companies.tableName == "COMPANY")
            assert(Companies.columns.map { it.name }.toSet() == setOf("ID", "NAME"))
            assert(UserProfiles.tableName == "USER_PROFILE")
            assert(UserProfiles.columns.map { it.name }.toSet() == setOf("ID", "PUBLIC_EMAIL", "PROFILE_PICTURE", "COMPANY_ID"))
        }
    """.trimIndent(), "ktorm.dbNamingStrategy" to "org.ktorm.ksp.compiler.util.UpperSnakeCaseDatabaseNamingStrategy")
}
