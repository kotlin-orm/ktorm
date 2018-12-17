'use strict';

const mockTraverse = jest.fn();
const mockUpdateResult = jest.fn();


jest.mock('../../../lib/traverse', () => ({
  traverse: mockTraverse
}));

jest.mock('../../../lib/updateResult', () => ({
  updateResult: mockUpdateResult
}));

const processArray = require('../../../lib/processItems/array');

describe('swagger-parser.lib.processItems.array', () => {
  test('should process array', () => {
    const object = {
      items: ['foo', 'bar']
    };
    const key = 'key';
    const isRequired = true;
    const result = {};

    processArray({object, key, isRequired, result});

    expect(mockUpdateResult).toHaveBeenCalled();
    expect(mockUpdateResult).toHaveBeenCalledWith({object, key, isRequired, result});

    expect(mockTraverse).toHaveBeenCalled();
    expect(mockTraverse).toHaveBeenCalledWith({object: object.items , key: key + '[]'});

  });
});
