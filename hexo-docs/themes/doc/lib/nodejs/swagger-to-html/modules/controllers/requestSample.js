'use strict';
const { highlight } = require('../../helpers');

const requestSample = (ctx) => {
  const sample = {
    header: {},
    formData: {},
    body: ''
  };

  const request = ctx.request;
  const baseUrl = ctx.baseUrl;
  let path = ctx.path;
  const verb = ctx.verb.toUpperCase();

  const pathParams = request.path;
  const queryParams = request.query;

  // Headers
  for (const header of request.header){
    sample.header[header.name] = header['x-example'];
  }

  // Form data
  for (const formData of request.formData){
    sample.formData[formData.name] = formData['x-example'];
  }

  // Update path with path params
  pathParams.forEach((param) => {
    if (param['x-example']){
      path = path.replace('{' + param.name + '}', param['x-example']);
    }
  });

  // Prepare query string
  const queryArray = [];
  queryParams.forEach((param) => {
    if (param['x-example']){
      queryArray.push(param.name + '=' + param['x-example']);
    }
  });
  const queryString = queryArray.join('&');
  if (queryString){
    path += '?' + queryString;
  }

  // Request body
  if (request.body.length){
    const _body = request.body[0]['x-examples'] && request.body[0]['x-examples']['default'];
    sample.body = JSON.stringify(_body, null, 2);
  }


  // Create CURL string
  let curlString = 'curl -v -X ' + verb + ' ' + baseUrl.replace(/\/$/, '') + path + '  \\\n';

  Object.keys(sample.header).forEach((header) => {
    if ('Authorization' === header){
      curlString += '-H "Authorization: Bearer <Access-Token>"  \\\n';
    } else {
      curlString += '-H "' + header + ': ' + sample.header[header] + '"  \\\n';
    }
  });

  Object.keys(sample.formData).forEach((data) => {
    curlString += '-F "' + data + ': ' + sample.formData[data] + '"  \\\n';
  });

  if (sample.body){
    curlString += sample.body;
  }

  curlString = curlString.replace(/\\\n$/, '');
  curlString = highlight({
    code: curlString,
    lang: 'bash'
  });

  return {
    curlString
  };
};


module.exports = requestSample;
