'use strict';

const operationController = require('../operation');

describe('controllers.operation', () => {
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

    const updatedCtx = operationController(ctx);
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

    const updatedCtx = operationController(ctx);
    const expectedSecurity = {
      foo: 'bar',
      items: ['a', 'b', 'c', 'd']
    };

    expect(updatedCtx.operation.security).toEqual(expectedSecurity);
  });
});
