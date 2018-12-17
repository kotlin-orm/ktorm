'use strict';


const { toSlug } = require('../../helpers');

const operations = (ctx) => {
  const paths = ctx.paths;
  const globalSecurity = ctx.security;

  ctx.schemes = ctx.schemes || [];

  let baseUrl = '';
  if (ctx.schemes.includes('https')){
    baseUrl += 'https://';
  } else {
    baseUrl += 'http://';
  }
  baseUrl += ctx.host + ctx.basePath;

  const operations  = Object.keys(paths).reduce((acc, path_) => {
    const ops = Object.keys(paths[path_]).map((verb) => {
      const operation = paths[path_][verb];
      const title = operation.summary || operation.operationId || operation.description || '';
      return Object.assign({}, operation, {
        verb,
        path: path_,
        title: title,
        id: toSlug(title)
      });
    });
    return acc.concat(ops);
  }, []);

  return {
    operations,
    baseUrl,
    globalSecurity
  };
};


module.exports = operations;
