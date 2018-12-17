'use strict';

const { isExclude } = require('../utils');
const { filter } = require('lodash');


/**
 * Filter parameters.
 */
const filterParameters = (swagger) => {

  const paramsToRemove = [];
  // Filter swager parameter definitions
  swagger.parameters && Object
    .keys(swagger.parameters)
    .forEach((key) => {
      const value = swagger.parameters[key];
      if (isExclude(value)){
        paramsToRemove.push(value.name);
        delete swagger.parameters[key];
      }
    });


  /**
   * Filter params for operations
   * */
  swagger.paths && Object
    .keys(swagger.paths)
    .forEach((key) => {
      const path = swagger.paths[key];
      path && Object
        .keys(path)
        .forEach((verb) => {
          const operation = path[verb];
          if ('object' !== typeof operation){
            return;
          }

          const parameters = operation.parameters;
          if (Array.isArray(parameters)){
            operation.parameters = filter(parameters, parameter => !paramsToRemove.includes(parameter.name) && !isExclude(parameter));
          }
        });
    });

  return swagger;
};


module.exports = filterParameters;
