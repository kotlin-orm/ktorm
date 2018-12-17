'use strict';

describe('swagger.index', () => {
  it('should expose swagger and decorators', () => {

    const swagger = require('../index');

    expect(Object.keys(swagger).length).toBe(2);
    expect(swagger.Swagger).toBeDefined();
    expect(swagger.decorators).toBeDefined();
  });

});
