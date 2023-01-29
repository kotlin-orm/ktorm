
plugins {
    id("kotlin")
}

val licenseHeaderText = """
/*
 * Copyright 2018-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
""".trimIndent()

val checkSourceHeader by tasks.registering {
    doLast {
        val sources = sourceSets.main.get()

        for (dir in sources.allSource.srcDirs) {
            val tree = fileTree(dir)
            tree.include("**/*.kt")

            tree.visit {
                if (file.isFile && !file.readText().startsWith(licenseHeaderText)) {
                    throw IllegalStateException("Copyright header not found in file: $file")
                }
            }
        }
    }
}

tasks.check {
    dependsOn(checkSourceHeader)
}
