'use strict';

const fs = require('fs');
const pkg = require('./package.json');
const path = require('path');

const banner = ` /*!
  * ${pkg.name} - ${pkg.version}
  * Copyright (c) see LICENSE at ${pkg.homepage}/blob/master/LICENSE
  */
`;

const files = [
  'source/style/doc.css',
  'source/style/swagger-ui-v2.css',
  'source/style/swagger-ui-v3.css',
  'source/script/doc.js'
];

files.forEach(writeBanner(banner));

function writeBanner (banner) {
  return function (filePath) {
    const fileExt = path.extname(filePath);
    const absFilePath = path.resolve(__dirname, filePath);
    let content = fs.readFileSync(absFilePath, 'utf8');
    content = banner + content;
    if (fileExt === '.css') {
      content = content.replace(/@charset "UTF-8";/g, '');
      content = '@charset "UTF-8";\n' + content;
    }
    fs.writeFileSync(absFilePath, content);
  };
}
