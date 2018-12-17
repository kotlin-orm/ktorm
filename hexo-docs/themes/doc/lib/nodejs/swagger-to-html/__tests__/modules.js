'use strict';

const mockGetBody = jest.fn(() =>  'flatBody');
const mockSwaggerParser = {
  getBody: mockGetBody
};

jest.mock('../../swagger-parser', () => mockSwaggerParser);

const mockJsonMarkup = jest.fn(json => json);
jest.mock('json-markup', () => mockJsonMarkup);

const modules = require('../modules');

describe('swagger-processor.modules', () => {
  describe('operations.controller', () => {
    it('should transform the context as expected', () => {
      const ctx = {
        paths: {
          '/foo/{fooId}': {
            'get': {
              summary: 'Get Foo'
            },
            'post': {
              summary: 'Post Foo'
            }
          },
          '/bar/{barId}': {
            'get': {
              summary: 'Get Bar'
            }
          },
        },
        host: 'example.com',
        basePath: '/',
        schemes: ['https']
      };

      const {operations, baseUrl} = modules.operations.controller(ctx);

      expect(operations).toBeInstanceOf(Array);
      expect(operations.length).toBe(3);

      expect(operations[0].title).toBe('Get Foo');
      expect(operations[0].verb).toBe('get');
      expect(operations[0].path).toBe('/foo/{fooId}');

      expect(operations[1].title).toBe('Post Foo');
      expect(operations[1].verb).toBe('post');
      expect(operations[1].path).toBe('/foo/{fooId}');

      expect(operations[2].title).toBe('Get Bar');
      expect(operations[2].verb).toBe('get');
      expect(operations[2].path).toBe('/bar/{barId}');

      expect(baseUrl).toBe('https://example.com/');

    });
  });

  describe('operation.controller', () => {
    test('should transform the context as expected', () => {
      const ctx = {
        operation: {
          security: {
            items: ['a']
          }
        },
        globalSecurity: {
          items: ['b'],
          foo: 'bar'
        }
      };

      const updatedCtx = modules.operation.controller(ctx);
      const expectedSecurity = {
        foo: 'bar',
        items: ['a', 'b']
      };

      expect(updatedCtx.operation.security).toEqual(expectedSecurity);
    });
    test('should handle duplicates', () => {
      const ctx = {
        operation: {
          security: {
            items: ['a', 'b', 'c']
          }
        },
        globalSecurity: {
          items: ['b', 'd'],
          foo: 'bar'
        }
      };

      const updatedCtx = modules.operation.controller(ctx);
      const expectedSecurity = {
        foo: 'bar',
        items: ['a', 'b', 'c', 'd']
      };

      expect(updatedCtx.operation.security).toEqual(expectedSecurity);
    });
  });

  describe('request.controller', () => {
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

      const {request} = modules.request.controller(ctx);

      expect(request).toBeInstanceOf(Object);
      expect(Object.keys(request).length).toBe(5);
      expect(request.header[0]).toBe(headerParam);
      expect(request.path[0]).toBe(pathParam);
      expect(request.query[0]).toBe(queryParam);
      expect(request.formData[0]).toBe(formDataParam);
      expect(request.body[0]).toBe(bodyParam);
    });
  });
  describe('requestBody.controller', () => {
    it('should transform the context as expected', () => {
      const ctx = {
        request: {},
        body: [
          {},
          {}
        ]
      };
      const {body} = modules.requestBody.controller(ctx);
      expect(body).toBeInstanceOf(Array);
      expect(body.length).toBe(2);
      expect(body[0]).toBe('flatBody');
    });
  });
  describe('requestSample.controller', () => {
    it('should transform the context as expected', () => {
      const requestBody = {
        'data': {
          'type': 'Order',
          'id': '123'
        }
      };

      const ctx = {
        'request': {
          'header': [
            {
              'name': 'Authorization',
              'in': 'header',
              'description': 'JWT token',
              'required': true,
              'type': 'string',
              'format': 'JWT token',
              'x-example': 'ThisIsAnAuthToken'
            },
            {
              'name': 'X-Flow-Id',
              'in': 'header',
              'description': 'For troubleshooting',
              'required': false,
              'type': 'string',
              'format': 'uuid',
              'x-example': 'ThisIsXFLOWID'
            }
          ],
          'path': [
            {
              'name': 'mid',
              'in': 'path',
              'description': 'To identify merchant this operation is carried out for',
              'required': true,
              'type': 'string',
              'format': 'uuid',
              'x-example': 'ThisIsMerchantId'
            },
            {
              'name': 'oid',
              'in': 'path',
              'description': 'The ID of the order for which the items are being requested.',
              'required': true,
              'type': 'string',
              'format': 'uuid',
              'x-example': 'ThisIsAnOrderId'
            }
          ],
          'query': [
            {
              'name': 'include',
              'in': 'query',
              'description': 'Comma seperated field names to be included',
              'required': false,
              'type': 'string',
              'x-example': 'ThisIsInclude'
            }
          ],
          'formData': [
            {
              'name': 'file',
              'in': 'formData',
              'description': "The merchant's document to create",
              'required': true,
              'type': 'file',
              'x-example': 'ThisIsInclude'
            },
            {
              'name': 'document-type',
              'in': 'formData',
              'description': "The merchant's document to create",
              'required': true,
              'type': 'string',
              'x-example': 'ThisIsInclude'
            }
          ],
          'body': [{
            'in': 'body',
            'name': 'order',
            'description': 'Patch of order',
            'required': true,
            'schema': {},
            'x-examples': {
              'default': requestBody
            }
          }]
        },
        'path': '/some/path/{mid}/{oid}',
        'verb': 'get',
        'baseUrl': 'https://example.com/'
      } ;

      const {sample, path, verb, baseUrl} = modules.requestSample.controller(ctx);

      expect(baseUrl).toBe('https://example.com/');
      expect(path).toBe('/some/path/ThisIsMerchantId/ThisIsAnOrderId?include=ThisIsInclude');
      expect(verb).toBe('GET');

      const expectedSample = {
        header: {
          'Authorization': 'ThisIsAnAuthToken',
          'X-Flow-Id': 'ThisIsXFLOWID'
        },
        formData: {
          'document-type': 'ThisIsInclude',
          'file': 'ThisIsInclude'
        },
        body: JSON.stringify(requestBody, null, 2)
      };

      expect(sample).toEqual(expectedSample);
    });
  });
  describe('response.controller', () => {
    it('should transform context as expected', () => {
      const responseData = {
        'data': {
        },
        'links': {
        }
      };
      const ctx = {
        sample: {
          'application/json': responseData
        }
      };

      const {sample} = modules.responseSample.controller(ctx);

      expect(sample).toEqual(responseData);
      expect(mockJsonMarkup).toBeCalled();

    });
  });
  describe('security.controller', () => {
    test('should return the context as expected', () => {
      const ctx = {
        securityDefinitions: 'securityDefinitions'
      };

      const updatedCtx = modules.security.controller(ctx);
      const {securityDefinitions} = updatedCtx;

      expect(Object.keys(updatedCtx).length).toBe(1);
      expect(securityDefinitions).toBe('securityDefinitions');

    });
  });
});
