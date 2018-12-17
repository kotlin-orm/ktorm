'use strict';


const { parseBody } = require('../../helpers');


const requestBody = ( ctx ) => {
  const body = [];

  ctx['body'].forEach((item) => {
    body.push(parseBody(item));
  });

  return {
    body
  };
};


module.exports = requestBody;
