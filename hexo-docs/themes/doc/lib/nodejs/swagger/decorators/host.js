'use strict';

const docKey = 'x-doc';
const hostKey = 'host';

const host = (swagger) => {

  let host;

  if (swagger && swagger[docKey] && swagger[docKey][hostKey]){
    host = swagger[docKey][hostKey];
    delete swagger[docKey][hostKey];
  }
  // Add more ways to provide host and order them in priority.

  if (host){
    swagger.host = host;
  }

  return swagger;
};


module.exports = host;
