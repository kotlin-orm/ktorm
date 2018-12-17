'use strict';

const mockParseResponse = jest.fn(() =>  'flatResponse');
const mockHelpers = {
  parseResponse: mockParseResponse
};

jest.mock('../../../helpers', () => mockHelpers);


const responseController = require('../response');

describe('controllers.resposne', () => {
  it('should transform context as expected', () => {
    const ctx = {
      'responseCode': 200,
      'response': {
        description: 'foo'
      }
    };

    const response = responseController(ctx);
    const expectedResponse = {
      description: 'foo',
      data: 'flatResponse'
    };

    expect(Object.keys(response).length).toBe(2);
    expect(response.responseCode).toBe(200);
    expect(response.response).toEqual(expectedResponse);

  });
});
