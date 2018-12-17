'use strict';

const {init} = require('./init');

const parseResponse = (response) => {

  if (!response){
    return {};
  }

  const schema = response.schema;

  if (schema){
    return init({ object: schema });
  } else {
    response.__noData = true;
    return response;
  }
};

module.exports = {
  parseResponse
};
