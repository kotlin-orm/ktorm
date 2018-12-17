'use strict';

const securityController = require('../security');

describe('security.controller', () => {
  test('should return the context as expected', () => {
    const ctx = {
      securityDefinitions: 'securityDefinitions'
    };

    const updatedCtx = securityController(ctx);
    const {securityDefinitions} = updatedCtx;

    expect(Object.keys(updatedCtx).length).toBe(1);
    expect(securityDefinitions).toBe('securityDefinitions');

  });
});
