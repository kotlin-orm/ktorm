'use strict';


const security = (ctx) => {
  const securityDefinitions = ctx.securityDefinitions;

  return {
    securityDefinitions
  };
};


module.exports = security;
