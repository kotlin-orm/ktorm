'use strict';

/**
 * Key for exclude flag.
 */
const docKey = 'x-doc';
const excludeKey = 'excluded';

/**
 * Function to decide what objects to exclude from the schema.
 */
const isExclude = (subject) => {
  if (subject.hasOwnProperty(docKey)){
    const isExcludable = !!(subject[docKey] && subject[docKey][excludeKey]);
    // Delete the key as its not a standard swagger key and will cause issues while rendering in SwaggerUI or Swagger-to-html
    delete subject[docKey];
    return isExcludable;
  }
  return false;
};

module.exports = {
  isExclude
};
