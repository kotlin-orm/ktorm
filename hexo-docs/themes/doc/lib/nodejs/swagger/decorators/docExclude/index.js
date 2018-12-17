'use strict';

const { filterSecurity, filterParameters, filterPaths, filterOperations} = require('./filters');

const docExclude = (swagger) => {

  swagger = filterSecurity(swagger);

  swagger = filterParameters(swagger);

  swagger = filterPaths(swagger);

  swagger = filterOperations(swagger);

  return  swagger;
};


module.exports = docExclude;
