'use strict';

const requestController = require('../request');

describe('controllers.request', () => {
  it('should transform the context as expected', () => {
    const headerParam = {
      name: 'headerParam',
      in: 'header',
      description: 'description',
      required: 'true',
      type: 'string',
      format: 'format',
    };
    const pathParam = {
      name: 'pathParam',
      in: 'path',
      description: 'description',
      required: 'true',
      type: 'string',
      format: 'format',
    };
    const queryParam = {
      name: 'queryParam',
      in: 'query',
      description: 'description',
      required: 'true',
      type: 'string',
      format: 'format',
    };
    const formDataParam = {
      name: 'formDataParam',
      in: 'formData',
      description: 'description',
      required: 'true',
      type: 'string',
      format: 'format',
    };
    const bodyParam = {
      name: 'bodyParam',
      in: 'body',
      description: 'description',
      required: 'true',
      type: 'string',
      format: 'format',
    };
    const ctx = {
      operations: [ {} ],
      operation: {
        summary: 'A nice summary',
        description: 'A nice description',
        parameters: [
          headerParam,
          pathParam,
          queryParam,
          formDataParam,
          bodyParam
        ],
        tags: [],
        produces: [],
        responses: {},
        verb: 'get',
        path: '/path',
        title: 'title',
      }
    };

    const {request} = requestController(ctx);

    expect(request).toBeInstanceOf(Object);
    expect(Object.keys(request).length).toBe(5);
    expect(request.header[0]).toBe(headerParam);
    expect(request.path[0]).toBe(pathParam);
    expect(request.query[0]).toBe(queryParam);
    expect(request.formData[0]).toBe(formDataParam);
    expect(request.body[0]).toBe(bodyParam);
  });
});
