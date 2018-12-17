'use strict';

const path = require('path');
const controllers = require('./controllers');


module.exports = {
  head: {
    order: 0,
    template: path.resolve(__dirname, './templates/head.ejs')
  },
  operations: {
    order: 2,
    template: path.resolve(__dirname, './templates/operations.ejs'),
    controller: controllers.operations
  },
  operation: {
    include: true,
    template: path.resolve(__dirname, './templates/operation.ejs'),
    controller: controllers.operation
  },
  header: {
    include: true,
    template: path.resolve(__dirname, './templates/header.ejs')
  },
  requestParams: {
    include: true,
    template: path.resolve(__dirname, './templates/requestParams.ejs')
  },
  requestBody: {
    include: true,
    template: path.resolve(__dirname, './templates/requestBody.ejs'),
    controller: controllers.requestBody
  },
  request: {
    include: true,
    template: path.resolve(__dirname, './templates/request.ejs'),
    controller: controllers.request
  },
  responses: {
    include: true,
    template: path.resolve(__dirname, './templates/responses.ejs'),
    controller: controllers.responses
  },
  response: {
    include: true,
    template: path.resolve(__dirname, './templates/response.ejs'),
    controller: controllers.response
  },
  emptyResponse: {
    include: true,
    template: path.resolve(__dirname, './templates/emptyResponse.ejs')
  },
  responseBody: {
    include: true,
    template: path.resolve(__dirname, './templates/responseBody.ejs')
  },
  requestSample: {
    include: true,
    template: path.resolve(__dirname, './templates/requestSample.ejs'),
    controller: controllers.requestSample
  },
  responseSample: {
    include: true,
    template: path.resolve(__dirname, './templates/responseSample.ejs'),
    controller: controllers.responseSample
  },
  security: {
    order: 1,
    template: path.resolve(__dirname, './templates/security.ejs'),
    controller: controllers.security
  },
  securityDefinition: {
    include: true,
    template: path.resolve(__dirname, './templates/securityDefinition.ejs')
  },
  securityRequirement: {
    include: true,
    template: path.resolve(__dirname, './templates/securityRequirement.ejs')
  },
  requestContent: {
    include: true,
    template: path.resolve(__dirname, './templates/requestContent.ejs')
  },
  formData: {
    include: true,
    template: path.resolve(__dirname, './templates/formData.ejs')
  }
};
