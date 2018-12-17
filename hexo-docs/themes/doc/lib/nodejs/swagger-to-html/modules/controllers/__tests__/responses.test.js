
'use strict';

const responsesController = require('../responses');

describe('controllers.resposnes', () => {
  it('should transform context as expected', () => {
    const ctx = {
      operation: {
        responses: 'foo'
      }
    };

    const { responses } = responsesController(ctx);

    expect(responses).toBe('foo');
  });
});
