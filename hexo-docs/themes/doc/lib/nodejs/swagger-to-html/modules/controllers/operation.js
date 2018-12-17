'use strict';


const { deepMerge } = require('../../helpers');

const operation = (ctx) => {
  if (ctx.operation.security){
    deepMerge(ctx.operation.security, ctx.globalSecurity);
  } else {
    ctx.operation.security = ctx.globalSecurity;
  }

  return ctx;
};


module.exports = operation;
