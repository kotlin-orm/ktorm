#!/usr/bin/env bash

./gradlew printClasspath

rm -rf ./docs/source/api-docs

java \
    -jar ./docs/tools/dokka-fatjar-with-hexo-format-0.9.18-SNAPSHOT.jar \
    -src ./ktorm-core/src/main/kotlin:./ktorm-jackson/src/main/kotlin:./ktorm-support-mysql/src/main/kotlin:./ktorm-support-oracle/src/main/kotlin:./ktorm-support-postgresql/src/main/kotlin:./ktorm-support-sqlite/src/main/kotlin:./ktorm-support-sqlserver/src/main/kotlin \
    -format hexo \
    -classpath $(cat build/ktorm.classpath) \
    -jdkVersion 8 \
    -include ./packages.md \
    -output ./docs/source/ \
    -module api-docs \
    -srcLink ktorm-core/src/main/kotlin=https://github.com/vincentlauvlwj/Ktorm/blob/master/ktorm-core/src/main/kotlin#L^^ktorm-jackson/src/main/kotlin=https://github.com/vincentlauvlwj/Ktorm/blob/master/ktorm-jackson/src/main/kotlin#L^^ktorm-support-mysql/src/main/kotlin=https://github.com/vincentlauvlwj/Ktorm/blob/master/ktorm-support-mysql/src/main/kotlin#L^^ktorm-support-oracle/src/main/kotlin=https://github.com/vincentlauvlwj/Ktorm/blob/master/ktorm-support-oracle/src/main/kotlin#L^^ktorm-support-postgresql/src/main/kotlin=https://github.com/vincentlauvlwj/Ktorm/blob/master/ktorm-support-postgresql/src/main/kotlin#L^^ktorm-support-sqlite/src/main/kotlin=https://github.com/vincentlauvlwj/Ktorm/blob/master/ktorm-support-sqlite/src/main/kotlin#L^^ktorm-support-sqlserver/src/main/kotlin=https://github.com/vincentlauvlwj/Ktorm/blob/master/ktorm-support-sqlserver/src/main/kotlin#L \
    -links https://docs.spring.io/spring/docs/current/javadoc-api/^https://docs.spring.io/spring/docs/current/javadoc-api/package-list^^https://fasterxml.github.io/jackson-databind/javadoc/2.9/^https://fasterxml.github.io/jackson-databind/javadoc/2.9/package-list^^https://fasterxml.github.io/jackson-core/javadoc/2.9/^https://fasterxml.github.io/jackson-core/javadoc/2.9/package-list^^https://fasterxml.github.io/jackson-annotations/javadoc/2.9/^https://fasterxml.github.io/jackson-annotations/javadoc/2.9/package-list^^https://www.slf4j.org/apidocs/^https://www.slf4j.org/apidocs/package-list^^http://commons.apache.org/proper/commons-logging/javadocs/api-release/^http://commons.apache.org/proper/commons-logging/javadocs/api-release/package-list

cd ./docs/

cd themes/doc && npx webpack -p && cd ../..

hexo clean && hexo generate

zip -r ktorm-docs.zip ./public

scp ktorm-docs.zip root@liuwj.me:~/ktorm-docs.zip

ssh root@liuwj.me 'unzip ktorm-docs.zip && rm -rf ktorm-docs && mv public ktorm-docs && rm ktorm-docs.zip'

rm ktorm-docs.zip
