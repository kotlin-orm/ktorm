'use strict';
const jsYaml = require('js-yaml');

const {referencedSchema, dereferencedSchema, mergedSchema} = require('./swagger-mocks.js');

describe('swagger.Swagger', () => {

  const mockValidate = () => Promise.resolve(dereferencedSchema);
  const mockBundle = () => Promise.resolve(referencedSchema);

  jest.mock('swagger-parser', () => ({
    validate: mockValidate,
    bundle: mockBundle
  }));

  const Swagger = require('../swagger');

  it('should expose swagger object', () => {
    expect(Swagger).toBeDefined();
  });

  describe('when swagger path is not passed', () => {
    it('should throw error', () => {
      try {
        new Swagger() ;
        fail('Swagger didn\'t throw error when passing no argument');  // eslint-disable-line no-undef
      } catch (error){
        expect(error).toEqual(new TypeError('Please provide path for swagger schema or a valid swagger object.'));
      }
    });
  });

  describe('when swagger path is passed', () => {
    it('should create a swagger object', () => {
      const swagger = new Swagger('path/to/swagger');
      expect(typeof swagger).toBe('object');
    });

    describe('when calling validate', () => {
      const swagger = new Swagger('path/to/swagger');


      it('should validate the schema and return the object', async () => {
        await expect(swagger.validate())
          .resolves
          .toEqual({
            options: {},
            swaggerInput: 'path/to/swagger',
            swaggerObject: dereferencedSchema
          });
      });
    });

    describe('when calling bundle', () => {
      const swagger = new Swagger('path/to/swagger');


      it('should return references schema', async () => {
        await expect(swagger.bundle())
          .resolves
          .toEqual({
            options: {},
            swaggerInput: 'path/to/swagger',
            swaggerObject: referencedSchema
          });
      });
    });

    describe('should decorate swagger using passed decorator', () => {
      const swagger = new Swagger('path/to/swagger');


      const decorator = () => ({schema: 'DECORATED_SWAGGER'});

      it('should return references schema', async () => {
        await expect(swagger.decorate(decorator))
          .resolves
          .toEqual({
            options: {},
            swaggerInput: 'path/to/swagger',
            swaggerObject: {schema: 'DECORATED_SWAGGER'}
          });
      });
    });

    describe('when getting swaggerJson', () => {
      const swagger = new Swagger('path/to/swagger');

      it('should return references schema', () => {
        return swagger
          .validate()
          .then((swagger) => {
            const swaggerJson = swagger.swaggerJson;
            expect(swaggerJson).toEqual(JSON.stringify(dereferencedSchema));
          });
      });
    });
    describe('when getting swaggerYaml', () => {
      const swagger = new Swagger('path/to/swagger');

      it('should return references schema', () => {
        return swagger
          .validate()
          .then((swagger) => {
            const swaggerYaml = swagger.swaggerYaml;
            expect(swaggerYaml).toEqual(jsYaml.safeDump(dereferencedSchema));
          });
      });
    });

    describe('when calling merge', () => {
      const swagger = new Swagger('path/to/swagger');

      it('should merge referenced and dereferenced schema', async () => {
        await expect(swagger.merge())
          .resolves
          .toEqual({
            options: {},
            swaggerInput: 'path/to/swagger',
            swaggerObject: mergedSchema
          });
      });
    });

    describe('when calling unmerge(with refrences) after merge', () => {
      const swagger = new Swagger('path/to/swagger');
      const dereferenced = false;

      it('should return referenced schema', () => {
        return swagger
          .merge()
          .then(swagger =>  swagger.unmerge(dereferenced))
          .then(swagger => {
            const swaggerObject = swagger.swaggerObject;
            expect(swaggerObject).toEqual(referencedSchema);
          });
      });
    });

    describe('when calling unmerge(dereferenced) after merge', () => {
      const swagger = new Swagger('path/to/swagger');
      const dereferenced = true;

      it('should return referenced schema', () => {
        return swagger
          .merge()
          .then(swagger =>  swagger.unmerge(dereferenced))
          .then(swagger => {
            const swaggerObject = swagger.swaggerObject;
            expect(swaggerObject).toEqual(dereferencedSchema);
          });
      });
    });
  });

});
