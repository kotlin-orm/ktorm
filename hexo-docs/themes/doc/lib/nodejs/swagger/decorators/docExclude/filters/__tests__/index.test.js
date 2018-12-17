'use strict';

describe('docExclude.index', () => {
  it('should expose filters', () => {
    const filters = require('../index');

    expect(Object.keys(filters).length).toBe(4);
    expect(filters.filterOperations).toBeDefined();
    expect(filters.filterParameters).toBeDefined();
    expect(filters.filterPaths).toBeDefined();
    expect(filters.filterSecurity).toBeDefined();
  });
});

