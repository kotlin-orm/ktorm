'use strict';

const { isExclude } = require('../utils');


const filterPaths = (swagger) => {

  swagger.paths && Object
    .keys(swagger.paths)
    .forEach((key) => {
      const path = swagger.paths[key];
      if (isExclude(path)){
        delete swagger.paths[key];
      }
    });

  return swagger;
};


module.exports = filterPaths;
