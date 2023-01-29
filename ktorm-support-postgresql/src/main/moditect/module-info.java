module ktorm.support.postgresql {
    requires ktorm.core;
    exports org.ktorm.support.postgresql;
    provides org.ktorm.database.SqlDialect with org.ktorm.support.postgresql.PostgreSqlDialect;
}
