'use strict';

const mockProcessArray = jest.fn(() => {});
const mockProcessObject = jest.fn(() => {});
const mockProcessEnum = jest.fn(() => {});
const mockProcessDefault = jest.fn(() => {});

jest.mock('../../lib/processItems', () => ({
  processArray: mockProcessArray,
  processObject: mockProcessObject,
  processEnum: mockProcessEnum,
  processDefault: mockProcessDefault
}));

const {traverse} = require('../../lib/traverse');

describe('swagger-parser.lib.traverse', () => {
  describe('if type is array', () => {
    const args = {
      object: {
        type: 'array'
      },
      key: 'mockKey',
      isRequired: 'isRequired'
    };


    test('should call processArray', () => {

      traverse(args);

      args['result'] = undefined;

      expect(mockProcessArray).toHaveBeenCalled();
      expect(mockProcessArray).toHaveBeenCalledWith(args);
    });
  });

  describe('if type is object', () => {
    const args = {
      object: {
        type: 'object'
      },
      key: 'mockKey',
      isRequired: 'isRequired'
    };


    test('should call processObject', () => {

      traverse(args);

      args['result'] = undefined;

      expect(mockProcessObject).toHaveBeenCalled();
      expect(mockProcessObject).toHaveBeenCalledWith(args);
    });
  });

  describe('if type is enum', () => {
    const args = {
      object: {
        enum: []
      },
      key: 'mockKey',
      isRequired: 'isRequired'
    };


    test('should call processEnum', () => {

      traverse(args);

      args['result'] = undefined;

      expect(mockProcessEnum).toHaveBeenCalled();
      expect(mockProcessEnum).toHaveBeenCalledWith(args);
    });
  });

  describe('for default case', () => {
    const args = {
      object: {
        type: 'string'
      },
      key: 'mockKey',
      isRequired: 'isRequired'
    };


    test('should call processEnum', () => {

      traverse(args);

      args['result'] = undefined;

      expect(mockProcessDefault).toHaveBeenCalled();
      expect(mockProcessDefault).toHaveBeenCalledWith(args);
    });
  });

  describe('when init is true', () => {
    const args = {
      object: {
        type: 'string'
      },
      key: 'mockKey',
      isRequired: 'isRequired',
      init: true
    };


    test('should set result to {}', () => {

      traverse(args);

      args['result'] = {};

      delete args['init'];

      expect(mockProcessDefault).toHaveBeenCalled();
      expect(mockProcessDefault).toHaveBeenCalledWith(args);
    });
  });

  describe('when allOf is present', () => {
    const args = {
      object: {
        type: 'object',
        allOf: [
          {
            type: 'object',
            properties: {
              name: {
                type: 'string'
              }
            }
          },
          {
            required: ['id'],
            properties: {
              id: {
                type: 'integer',
                format: 'int64'
              }
            }
          }
        ]
      },
      key: 'mockKey',
      isRequired: 'isRequired',
      init: true
    };


    test('should parse allOf', () => {

      traverse(args);

      const expectedArgs = {
        object: {
          type: 'object',
          required: ['id'],
          properties: {
            name: {
              type: 'string'
            },
            id: {
              type: 'integer',
              format: 'int64'
            }
          }
        },
        key: 'mockKey',
        isRequired: 'isRequired',
        result: {}
      };

      expect(mockProcessObject).toHaveBeenCalled();
      expect(mockProcessObject).toHaveBeenCalledWith(expectedArgs);
    });
  });
});
