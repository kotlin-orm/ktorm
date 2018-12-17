'use strict';

const {init} = require('./init');

const parseBody = (body) => {

  if (!body || !body.schema){
    return {};
  }

  const schema = body.schema;

  return init({ object: schema });
};

module.exports = {
  parseBody
};
