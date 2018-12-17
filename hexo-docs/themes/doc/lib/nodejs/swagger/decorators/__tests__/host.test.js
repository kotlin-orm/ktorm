'use strict';


describe('decorators.host', () => {
  it('should update host', () => {
    const hostDecorator = require('../host');

    const swagger = {
      host: 'ORIGINAL_HOST',
      'x-doc': {
        host: 'UPDATED_HOST'
      }
    };

    const updatedSwagger = hostDecorator(swagger);

    const expectedSwagger = {
      host: 'UPDATED_HOST',
      'x-doc': {}
    };

    expect(updatedSwagger).toEqual(expectedSwagger);
  });
});
