'use strict';

const fs = require('fs');

describe('swager.Swagger.parser', () => {
  const specFile = 'lib/nodejs/swagger/__tests__/_petstore.yaml';
  const expectedSpec = 'lib/nodejs/swagger/__tests__/_petstore-parsed.yaml';

  const Swagger = require('../swagger');
  const decorators = require('../decorators');
  const swagger = new Swagger(specFile);

  it('should generate xml schema', () => {
    swagger
      .validate()
      .then(swagger =>  swagger.decorate(decorators.docExclude))
      .then(swagger =>  swagger.decorate(decorators.host))
      .then(swagger => {
        const readableStream = fs.createReadStream(expectedSpec);
        let data = '';

        readableStream
          .on('readable', () => {
            let chunk;
            while ((chunk = readableStream.read()) !== null) {
              data += chunk;
            }
          })
          .on('end', () => {
            expect(data).toEqual(swagger.swaggerYaml);
          });
      });
  });
});
