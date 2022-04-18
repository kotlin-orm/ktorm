
include("ktorm-core")
include("ktorm-global")
include("ktorm-jackson")
include("ktorm-support-mysql")
include("ktorm-support-oracle")
include("ktorm-support-postgresql")
include("ktorm-support-sqlite")
include("ktorm-support-sqlserver")

rootProject.name = "ktorm"
rootProject.children.forEach { project ->
    project.buildFileName = "${project.name}.gradle.kts"
}
