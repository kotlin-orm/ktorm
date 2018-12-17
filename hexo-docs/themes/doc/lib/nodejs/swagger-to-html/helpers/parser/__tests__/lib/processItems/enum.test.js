'use strict';

const mockUpdateResult = jest.fn();

jest.mock('../../../lib/updateResult', () => ({
  updateResult: mockUpdateResult
}));

const processEnum = require('../../../lib/processItems/enum');

describe('swagger-parser.lib.processItems.enum', () => {
  test('should process enum', () => {
    const object = {
      enum: [1, 2]
    };
    const key = 'key';
    const isRequired = true;
    const result = {};

    processEnum({object, key, isRequired, result});

    object.type = 'enum';
    object.values = object.enum;

    expect(mockUpdateResult).toHaveBeenCalled();
    expect(mockUpdateResult).toHaveBeenCalledWith({object, key, isRequired, result});
  });
});
