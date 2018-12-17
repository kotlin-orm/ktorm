'use strict';

const mockParseBody = jest.fn(() =>  'flatBody');
const mockHelpers = {
  parseBody: mockParseBody
};

jest.mock('../../../helpers', () => mockHelpers);


const requestBodyController = require('../requestBody');

describe('controllers.requestBody', () => {
  it('should transform the context as expected', () => {
    const ctx = {
      request: {},
      body: [
        {},
        {}
      ]
    };
    const {body} = requestBodyController(ctx);
    expect(body).toBeInstanceOf(Array);
    expect(body.length).toBe(2);
    expect(body[0]).toBe('flatBody');
  });
});

