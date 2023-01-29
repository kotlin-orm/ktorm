module ktorm.support.mysql {
    requires ktorm.core;
    exports org.ktorm.support.mysql;
    provides org.ktorm.database.SqlDialect with org.ktorm.support.mysql.MySqlDialect;
}
