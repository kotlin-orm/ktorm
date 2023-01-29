module ktorm.support.oracle {
    requires ktorm.core;
    exports org.ktorm.support.oracle;
    provides org.ktorm.database.SqlDialect with org.ktorm.support.oracle.OracleDialect;
}
