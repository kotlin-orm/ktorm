'use strict';

const { isExclude } = require('../utils');


/**
 * Filteres securitDefinitions and security requirements.
 */
const filterSecurity = (swagger) => {

  const securityToRemove = [];

  const securityDefinitions = swagger.securityDefinitions;

  const filterSecurityRequirements = (securityArr) => {
    return securityArr.reduce((acc, curr) => {
      const key = Object.keys(curr)[0];
      if (!securityToRemove.includes(key)){
        acc.push(curr);
      }
      return acc;
    },
    []);
  };

  // Remove security definitions
  securityDefinitions && Object
    .keys(securityDefinitions)
    .forEach((key) => {
      const value = securityDefinitions[key];
      if (isExclude(value)){
        securityToRemove.push(key);
        delete securityDefinitions[key];
      }
    });

  // Remove security requirements
  if (securityToRemove.length){

    // Global security
    if (swagger.security){
      swagger.security = filterSecurityRequirements(swagger.security);
    }

    // Local security
    swagger.paths && Object
      .keys(swagger.paths)
      .forEach((key) => {
        const path = swagger.paths[key];
        path && Object
          .keys(path)
          .forEach((verb) => {
            const operation = path[verb];
            if (operation.security){
              operation.security = filterSecurityRequirements(operation.security);
            }
          });
      });
  }

  return swagger;
};

module.exports = filterSecurity;
