module ktorm.support.sqlserver {
    requires ktorm.core;
    exports org.ktorm.support.sqlserver;
    provides org.ktorm.database.SqlDialect with org.ktorm.support.sqlserver.SqlServerDialect;
}
