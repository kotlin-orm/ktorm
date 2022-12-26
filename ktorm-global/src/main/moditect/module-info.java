module ktorm.global {
    requires ktorm.core;
    requires static spring.jdbc;
    requires static spring.tx;
    exports org.ktorm.global;
}
