'use strict';

const ParseSchemaFileError = require('./parse-schema-file-error.js');

function parseSchemaFile (filePath, pageSource, hexo) {

  const swaggerStore = require('../swagger-store')({hexo});
  try {
    return swaggerStore
      .getSwagger(filePath)
      .then(({swagger, downloadRoute}) => {
        return {
          downloadRoute,
          pageSource,
          swagger: swagger.swaggerJson
        };
      });
  } catch (error){
    return Promise.reject(new ParseSchemaFileError({
      'message': 'There is an error reading the file.',
      filePath,
      'referencePath': pageSource
    }));
  }
}

module.exports = parseSchemaFile;
