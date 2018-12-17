'use strict';


const responses = (ctx) => {
  const operation = ctx.operation;
  const responses = operation['responses'];

  return {
    responses
  };
};


module.exports = responses;
