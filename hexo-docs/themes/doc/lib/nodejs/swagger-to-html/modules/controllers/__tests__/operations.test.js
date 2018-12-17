'use strict';

const operationsController = require('../operations');

describe('controllers.operations', () => {
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

    const {operations, baseUrl} = operationsController(ctx);

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
