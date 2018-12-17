'use strict';


const { parseResponse} = require('../../helpers');


const response = (ctx) => {
  const responseCode = ctx['responseCode'];
  const originalResponse = ctx['response'];
  const responseData = parseResponse(originalResponse);
  const response = {
    description: originalResponse.description,
    data: responseData
  };

  return {
    responseCode,
    response
  };
};


module.exports = response;
