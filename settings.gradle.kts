
plugins {
    id("com.gradle.enterprise") version "3.14.1"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

include("ktorm-core")
include("ktorm-global")
include("ktorm-jackson")
include("ktorm-ksp-annotations")
include("ktorm-ksp-compiler")
include("ktorm-ksp-compiler-maven-plugin")
include("ktorm-ksp-spi")
include("ktorm-support-mysql")
include("ktorm-support-oracle")
include("ktorm-support-postgresql")
include("ktorm-support-snowflake")
include("ktorm-support-sqlite")
include("ktorm-support-sqlserver")

rootProject.name = "ktorm"
rootProject.children.forEach { project ->
    project.buildFileName = "${project.name}.gradle.kts"
}

gradleEnterprise {
    if (System.getenv("CI") == "true") {
        buildScan {
            publishAlways()
            termsOfServiceUrl = "https://gradle.com/terms-of-service"
            termsOfServiceAgree = "yes"
        }
    }
}
include("ktorm-support-snowflake")
