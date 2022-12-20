module ktorm.support.sqlite {
    requires ktorm.core;
    exports org.ktorm.support.sqlite;
    provides org.ktorm.database.SqlDialect with org.ktorm.support.sqlite.SQLiteDialect;
}
