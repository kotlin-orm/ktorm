'use strict';

const mockTraverse = jest.fn();
const mockUpdateResult = jest.fn();


jest.mock('../../../lib/traverse', () => ({
  traverse: mockTraverse
}));

jest.mock('../../../lib/updateResult', () => ({
  updateResult: mockUpdateResult
}));

const processObject = require('../../../lib/processItems/object');

describe('swagger-parser.lib.processItems.object', () => {
  describe('when object is blank', () => {
    test('should use default values', () => {
      const object = {};
      const key = 'key';
      const isRequired = true;
      const result = {};

      processObject({object, key, isRequired, result});

      expect(mockTraverse).toHaveBeenCalledTimes(0);

    });
  });

  describe('when there is no key', () => {
    test('should use propertyName as key', () => {
      const object = {
        properties: {
          foo: {
            type: 'string'
          }
        },
        required: ['foo']
      };
      const key = '';
      const isRequired = true;
      const result = {};

      processObject({object, key, isRequired, result});

      expect(mockUpdateResult).toHaveBeenCalled();
      expect(mockUpdateResult).toHaveBeenCalledWith({object, key, isRequired, result});

      expect(mockTraverse).toHaveBeenCalledTimes(1);
      expect(mockTraverse).toHaveBeenCalledWith({object: {type: 'string'} , key: 'foo', isRequired: true});

    });
  });

  describe('if object have additionalProperties', () => {
    test('should handle them', () => {
      const object = {
        additionalProperties: {
          properties: {
            foo: {
              type: 'string'
            }
          },
          required: ['foo']
        }
      };
      const key = '';
      const isRequired = true;
      const result = {};

      processObject({object, key, isRequired, result});

      expect(mockUpdateResult).toHaveBeenCalled();
      expect(mockUpdateResult).toHaveBeenCalledWith({object, key, isRequired, result});

      expect(mockTraverse).toHaveBeenCalledTimes(1);
      expect(mockTraverse).toHaveBeenCalledWith({object: {type: 'string'} , key: 'foo', isRequired: true});


    });
  });

  test('should process object', () => {
    const object = {
      properties: {
        foo: {
          type: 'string'
        },
        bar: {
          type: 'object'
        }
      },
      required: ['foo']
    };
    const key = 'key';
    const isRequired = true;
    const result = {};

    processObject({object, key, isRequired, result});

    expect(mockUpdateResult).toHaveBeenCalled();
    expect(mockUpdateResult).toHaveBeenCalledWith({object, key, isRequired, result});

    expect(mockTraverse).toHaveBeenCalledTimes(2);
    expect(mockTraverse).toHaveBeenCalledWith({object: {type: 'string'} , key: key + '.foo', isRequired: true});
    expect(mockTraverse).toHaveBeenCalledWith({object: {type: 'object'} , key: key + '.bar', isRequired: false});
  });
});
