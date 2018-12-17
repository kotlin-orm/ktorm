'use strict';

describe('decocators.index', () => {

  it('should expost decocators', () => {
    const decorators = require('../index');

    expect(Object.keys(decorators).length).toBe(2);

    expect(decorators.docExclude).toBeDefined();
    expect(decorators.host).toBeDefined();
  });
});
