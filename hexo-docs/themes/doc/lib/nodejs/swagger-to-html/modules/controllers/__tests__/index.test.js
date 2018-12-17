'use strict';

const index = require('../index');

describe('controllers.index', () => {
  it('should return controllers', () => {
    expect(Object.keys(index).length).toBe(9);
    expect(index.operation).toBeDefined();
    expect(index.operations).toBeDefined();
    expect(index.request).toBeDefined();
    expect(index.requestBody).toBeDefined();
    expect(index.requestSample).toBeDefined();
    expect(index.response).toBeDefined();
    expect(index.responses).toBeDefined();
    expect(index.responseSample).toBeDefined();
    expect(index.security).toBeDefined();
  });
});
