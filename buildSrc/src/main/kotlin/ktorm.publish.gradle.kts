
plugins {
    id("kotlin")
    id("signing")
    id("maven-publish")
}

val jarSources by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.map { it.allSource })
}

val jarJavadoc by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        create<MavenPublication>("dist") {
            from(components["java"])
            artifact(jarSources)
            artifact(jarJavadoc)

            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            pom {
                name.set("${project.group}:${project.name}")
                description.set("A lightweight ORM Framework for Kotlin with strong typed SQL DSL and sequence APIs.")
                url.set("https://www.ktorm.org")
                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                scm {
                    url.set("https://github.com/kotlin-orm/ktorm")
                    connection.set("scm:git:https://github.com/kotlin-orm/ktorm.git")
                    developerConnection.set("scm:git:ssh://git@github.com/kotlin-orm/ktorm.git")
                }
                developers {
                    developer {
                        id.set("vincentlauvlwj")
                        name.set("vince")
                        email.set("me@liuwj.me")
                    }
                    developer {
                        id.set("waluo")
                        name.set("waluo")
                        email.set("1b79349b@gmail.com")
                    }
                    developer {
                        id.set("clydebarrow")
                        name.set("Clyde")
                        email.set("clyde@control-j.com")
                    }
                    developer {
                        id.set("Ray-Eldath")
                        name.set("Ray Eldath")
                        email.set("ray.eldath@outlook.com")
                    }
                    developer {
                        id.set("hangingman")
                        name.set("hiroyuki.nagata")
                        email.set("idiotpanzer@gmail.com")
                    }
                    developer {
                        id.set("onXoot")
                        name.set("beetlerx")
                        email.set("beetlerx@gmail.com")
                    }
                    developer {
                        id.set("arustleund")
                        name.set("Andrew Rustleund")
                        email.set("andrew@rustleund.com")
                    }
                    developer {
                        id.set("afezeria")
                        name.set("afezeria")
                        email.set("zodal@outlook.com")
                    }
                    developer {
                        id.set("scorsi")
                        name.set("Sylvain Corsini")
                        email.set("sylvain.corsini@protonmail.com")
                    }
                    developer {
                        id.set("lyndsysimon")
                        name.set("Lyndsy Simon")
                        email.set("lyndsy@lyndsysimon.com")
                    }
                    developer {
                        id.set("antonydenyer")
                        name.set("Antony Denyer")
                        email.set("git@antonydenyer.co.uk")
                    }
                    developer {
                        id.set("mik629")
                        name.set("Mikhail Erkhov")
                        email.set("mikhail.erkhov@gmail.com")
                    }
                    developer {
                        id.set("sinzed")
                        name.set("Saeed Zahedi")
                        email.set("saeedzhd@gmail.com")
                    }
                    developer {
                        id.set("smn-dv")
                        name.set("Simon Schoof")
                        email.set("simon.schoof@hey.com")
                    }
                    developer {
                        id.set("pedrod")
                        name.set("Pedro Domingues")
                        email.set("pedro.domingues.pt@gmail.com")
                    }
                    developer {
                        id.set("efenderbosch")
                        name.set("Eric Fenderbosch")
                        email.set("eric@fender.net")
                    }
                    developer {
                        id.set("kocproz")
                        name.set("Kacper Stasiuk")
                        email.set("kocproz@pm.me")
                    }
                    developer {
                        id.set("2938137849")
                        name.set("ccr")
                        email.set("2938137849@qq.com")
                    }
                    developer {
                        id.set("zuisong")
                        name.set("zuisong")
                        email.set("com.me@foxmail.com")
                    }
                    developer {
                        id.set("svenallers")
                        name.set("Sven Allers")
                        email.set("sven.allers@gmx.de")
                    }
                    developer {
                        id.set("lookup-cat")
                        name.set("夜里的向日葵")
                        email.set("641571835@qq.com")
                    }
                    developer {
                        id.set("michaelfyc")
                        name.set("michaelfyc")
                        email.set("michael.fyc@outlook.com")
                    }
                    developer {
                        id.set("brohacz")
                        name.set("Michal Brosig")
                    }
                }
            }
        }

        repositories {
            maven {
                name = "central"
                url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
                credentials {
                    username = System.getenv("OSSRH_USER")
                    password = System.getenv("OSSRH_PASSWORD")
                }
            }
            maven {
                name = "snapshot"
                url = uri("https://oss.sonatype.org/content/repositories/snapshots")
                credentials {
                    username = System.getenv("OSSRH_USER")
                    password = System.getenv("OSSRH_PASSWORD")
                }
            }
        }
    }
}

signing {
    val keyId = System.getenv("GPG_KEY_ID")
    val secretKey = System.getenv("GPG_SECRET_KEY")
    val password = System.getenv("GPG_PASSWORD")

    setRequired {
        !project.version.toString().endsWith("SNAPSHOT")
    }

    useInMemoryPgpKeys(keyId, secretKey, password)
    sign(publishing.publications["dist"])
}
