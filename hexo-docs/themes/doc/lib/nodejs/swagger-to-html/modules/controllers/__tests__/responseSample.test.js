'use strict';

const mockHighlight = jest.fn(() => 'EXPECTED_SAMPLE');

jest.mock('../../../helpers', () => ({
  highlight: mockHighlight
}));

const responseSampleController = require('../responseSample');

describe('controllers.responseSample', () => {
  it('should transform context as expected', () => {
    const responseData = {
      'data': {
      },
      'links': {
      }
    };
    const ctx = {
      sample: {
        'application/json': responseData
      }
    };

    const {sample} = responseSampleController(ctx);

    const sampleStr = JSON.stringify(ctx.sample['application/json'], null, 2);

    const highlightArgs = {
      code: sampleStr,
      lang: 'json'
    };

    expect(mockHighlight).toHaveBeenCalled();
    expect(mockHighlight).toHaveBeenCalledWith(highlightArgs);

    expect(sample).toEqual('EXPECTED_SAMPLE');

  });
});
