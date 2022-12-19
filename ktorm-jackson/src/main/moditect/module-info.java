module ktorm.jackson {
    requires ktorm.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.kotlin;
    requires com.fasterxml.jackson.datatype.jsr310;
    exports org.ktorm.jackson;
}
