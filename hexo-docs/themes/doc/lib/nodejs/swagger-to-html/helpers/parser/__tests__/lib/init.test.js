'use strict';

const mockTraverse = jest.fn();

jest.mock('../../lib/traverse', () => ({
  traverse: mockTraverse
}));

const {init} = require('../../lib/init');

describe('swagger-parser.lib.init', () => {
  test('should init traverse', () => {
    const object = {foo: 'bar'};

    init({object});

    expect(mockTraverse).toHaveBeenCalled();
    expect(mockTraverse).toHaveBeenCalledWith({object, key: '', init: true});
  });

});
