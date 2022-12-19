module ktorm.core {
    // Basic dependencies.
//    requires transitive java.sql;
//    requires transitive java.sql.rowset;
//    requires transitive kotlin.stdlib;
//    requires transitive kotlin.reflect;

    // Optional dependencies.
//    requires static spring.tx;
//    requires static spring.jdbc;
//    requires static android;
//    requires static commons.logging;
//    requires static slf4j.api;
//    requires static postgresql;

    // Exported packages.
    exports org.ktorm.database;
    exports org.ktorm.dsl;
    exports org.ktorm.entity;
    exports org.ktorm.expression;
    exports org.ktorm.logging;
    exports org.ktorm.schema;
}
