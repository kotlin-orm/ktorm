'use strict';

describe('swagger-to-html', () => {

  const mockGetSwagger = jest.fn()
    .mockImplementationOnce(() => {
      return Promise.resolve({
        swagger: {
          swaggerObject: 'SWAGGER_OBJECT'
        }
      });
    });

  jest.mock('../../swagger-store', () => {
    return () => ({
      getSwagger: mockGetSwagger
    });
  });

  jest.mock('../core', () => {
    return {
      createTransformer: ({input}) => {
        return input();
      }
    };
  });
  const hexo = {};
  const transformerPromise = require('../index')({hexo});

  it('shouldn\'t emit any error', async () => {
    await expect(transformerPromise).resolves.toEqual('SWAGGER_OBJECT');
  });
});
