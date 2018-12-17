'use strict';


const request = (ctx) => {
  const operation = ctx.operation;
  const parameters = operation.parameters;
  const baseUrl = ctx.baseUrl;
  const path = operation.path;
  const verb = operation.verb;
  const request = {
    header: [],
    path: [],
    query: [],
    formData: [],
    body: []
  };

  parameters && parameters.forEach((param) => {
    const paramType = param['in'];
    request[paramType] && request[paramType].push(param);
  });

  return {
    request,
    path,
    verb,
    baseUrl
  };
};


module.exports = request;
