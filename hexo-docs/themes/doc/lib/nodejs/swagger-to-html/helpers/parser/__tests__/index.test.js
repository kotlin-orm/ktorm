'use strict';

describe('parser.index', () => {
  const parser = require('../index');

  test('should export getBody, getResponse and filterResponses', () => {
    const {parseBody, parseResponse} = parser;
    expect(Object.keys(parser).length).toBe(2);
    expect(parseBody).toBeInstanceOf(Function);
    expect(parseResponse).toBeInstanceOf(Function);
  });
});
