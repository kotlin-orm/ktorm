'use strict';

const mockUpdateResult = jest.fn();

jest.mock('../../../lib/updateResult', () => ({
  updateResult: mockUpdateResult
}));

const processDefault = require('../../../lib/processItems/default');

describe('swagger-parser.lib.processItems.default', () => {
  test('should process default values', () => {
    const object = {};
    const key = 'key';
    const isRequired = true;
    const result = {};

    processDefault({object, key, isRequired, result});

    expect(mockUpdateResult).toHaveBeenCalled();
    expect(mockUpdateResult).toHaveBeenCalledWith({object, key, isRequired, result});

  });
});
