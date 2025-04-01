module ktorm.support.snowflake {
    requires ktorm.core;
    exports org.ktorm.support.snowflake;
    provides org.ktorm.database.SqlDialect with org.ktorm.support.snowflake.SnowflakeDialect;
}
