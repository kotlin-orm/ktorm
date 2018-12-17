'use strict';

const { isExclude } = require('../utils');


const filterOperations = (swagger) => {

  swagger.paths && Object
    .keys(swagger.paths)
    .forEach((key) => {
      const path = swagger.paths[key] ;
      path && Object
        .keys(path)
        .forEach((verb) => {
          const operation = path[verb];
          if (isExclude(operation)){
            delete path[verb];
          }
        });
    });

  return swagger;
};


module.exports = filterOperations;
