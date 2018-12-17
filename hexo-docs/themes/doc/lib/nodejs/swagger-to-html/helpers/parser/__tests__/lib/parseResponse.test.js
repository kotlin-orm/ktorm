'use strict';

const mockInit = jest.fn(() => {});

jest.mock('../../lib/init', () => ({
  init: mockInit
}));

describe('swagger-parser.lib.parseResponse', () => {
  const {parseResponse} = require('../../lib/parseResponse');

  describe('if response not defined', () => {
    const parsedResponse = parseResponse();
    test('should return blank object', () => {
      expect(parsedResponse).toEqual({});
    });
  });

  describe('if response is defined', () => {
    describe('if response have schema', () => {
      const response = {
        schema: 'mockSchema'
      };
      parseResponse(response);
      expect(mockInit).toHaveBeenCalled();
      expect(mockInit).toHaveBeenCalledWith({object: response.schema});
    });

    describe('if response does not have schema', () => {
      const response = {};
      const parsedResponse = parseResponse(response);
      expect(parsedResponse).toEqual({ '__noData': true});
    });
  });
});
