'use strict';


describe('swagger-parser.lib.updateResult', () => {

  const {updateResult} = require('../../lib/updateResult');

  describe('for no key', () => {
    const result = {};
    const args = {};

    const output = updateResult(args);

    test('should do nothing and return false', () => {
      expect(output).toBe(false);
      expect(result).toEqual({});
    });
  });

  describe('when key is defined', () => {
    test('should update the result', () => {
      const result = {};
      const args = {
        object:{
          description: 'description',
          type: 'type',
          values: [1, 2],
          required: true,
          example: 'example',
          format: 'format'
        },
        key: 'key',
        result: result
      };

      updateResult(args);

      expect(result.key).toBeDefined();

      const _result = result.key;

      expect(_result.description).toBe('description');
      expect(_result.type).toBe('type');
      expect(_result.values).toEqual([1, 2]);
      expect(_result.required).toBe(true);
      expect(_result.example).toBe('example');
      expect(_result.format).toBe('format');
    });
  });

  describe('when isRequired is set', () => {
    test('should use it', () => {
      const result = {};
      const args = {
        object:{},
        key: 'key',
        result: result,
        isRequired: true
      };

      updateResult(args);

      expect(result.key).toBeDefined();

      const _result = result.key;

      expect(_result.required).toBe(true);
    });
  });



});
