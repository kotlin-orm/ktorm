/**
 * @jest-environment jsdom
 */

/* global global */
const mockSearcher = () => {
  return () => {};
};

jest.mock('../../../nodejs/search/searcher', () => mockSearcher);

const mockLunr = {
  Index: {
    load: (index) => index
  }
};

jest.mock('lunr', () => mockLunr);

const mockResponse = () => {
  return {
    json: () => Promise.resolve({
      index: {},
      store: {}
    })
  };
};

const mockFetch = jest.fn().mockReturnValue(Promise.resolve(mockResponse()));

global.fetch = mockFetch;

describe('browser.search.load', () => {
  const load = require('../load');

  it('should fetch the expected json file and return a search function', () => {
    return load('/foo.json').then((search) => {
      expect(mockFetch).toHaveBeenCalledWith('/foo.json', { credentials: 'include'} );
      expect(typeof search).toBe('function');
    });
  });

  it('should fetch the default json file and return a search function', () => {
    return load().then((search) => {
      expect(mockFetch).toHaveBeenCalledWith('/lunr.json', { credentials: 'include'} );
      expect(typeof search).toBe('function');
    });
  });
});
