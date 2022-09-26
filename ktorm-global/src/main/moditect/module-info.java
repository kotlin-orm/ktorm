module ktorm.global {
    requires ktorm.core;
    requires static spring.tx;
    requires static spring.jdbc;
    exports org.ktorm.global;
}
