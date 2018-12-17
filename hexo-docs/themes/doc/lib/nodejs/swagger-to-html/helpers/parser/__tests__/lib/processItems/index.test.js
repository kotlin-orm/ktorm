'use strict';

describe('swagger-parser.lib.processItems.index', () => {
  test('should export processors', () => {

    const index = require('../../../lib/processItems/index');

    expect(index.processEnum).toBeDefined();
    expect(index.processArray).toBeDefined();
    expect(index.processObject).toBeDefined();
    expect(index.processDefault).toBeDefined();
  });
});
