#!/usr/bin/env bash

rm -rf ./hexo-docs/source/api-docs

java \
    -jar ./hexo-docs/tools/dokka-fatjar-with-hexo-format-0.9.17-SNAPSHOT.jar \
    -src ./ktorm-core/src/main/kotlin:./ktorm-jackson/src/main/kotlin:./ktorm-support-mysql/src/main/kotlin:./ktorm-support-oracle/src/main/kotlin:./ktorm-support-postgresql/src/main/kotlin \
    -format hexo \
    -include ./packages.md \
    -output ./hexo-docs/source/ \
    -module api-docs

cd ./hexo-docs/

cd themes/doc && npx webpack -p && cd ../..

hexo clean && hexo generate

zip -r ktorm-docs.zip ./public

scp ktorm-docs.zip root@liuwj.me:~/ktorm-docs.zip

ssh root@liuwj.me 'unzip ktorm-docs.zip && rm -rf ktorm-docs && mv public ktorm-docs && rm ktorm-docs.zip'

rm ktorm-docs.zip