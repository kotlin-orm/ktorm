cp -rf public/ target/
cd target/
git add .
git commit -a -m "autopush"
git push
cd ..
