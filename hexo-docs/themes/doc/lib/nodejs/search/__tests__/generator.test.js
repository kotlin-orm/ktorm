'use strict';

jest.mock('../../hexo-util', () => mockHexoUtil);

const mockSend = jest.fn();
const mockFork = jest.fn()
  .mockImplementation(() => ({
    on: () => {},
    send: mockSend
  }));

jest.mock('child_process', () => ({
  fork: mockFork
}));

const {mockCb, mockHexo, mockHexoUtil} = require('./mocks');
const generator = require('../generator');

describe('search.generator', () => {
  let generate;

  beforeAll(() => {
    generate = generator({
      hexo: mockHexo
    });
  });

  it('should return a generate function', () => {
    expect(typeof generate).toBe('function');
  });

  describe('generate', () => {
    let result;
    beforeAll(() => {
      result = generate({
        pages: [
          {title: 'foo', content: '', path: '/foo', source: 'foo.md'},
          {title: 'bar', content: 'bar', path: '/bar', source: 'bar.md'}
        ]
      }, mockCb);
    });

    it('should return search `index` and `store` from pages', () => {
      expect(result.index).not.toBeUndefined();
      expect(result.store).not.toBeUndefined();
    });

    describe('index.search', () => {
      it('should return the expected results', () => {
        const results = result.index.search('b*');
        expect(results.length).toBeGreaterThan(0);
      });
    });

    it('store should contain only one specific entry, when "search:only" is used', () => {
      const result = generate({
        pages: [
          {title: 'foo', content: '', path: '/foo', source: 'foo.md'},
          {title: 'bar', content: 'bar', path: '/bar', source: 'bar.md', search: 'only'}
        ]
      }, () => {});

      expect(Object.keys(result.store).length).toBe(1);
      expect(result.store['/bar']).toBeDefined();
    });

    it('should throw an error when "pages" is not an array', () => {
      let error;
      try {
        generate({ pages: null }, () => {});
      } catch (err) {
        error = err;
      }
      expect(error instanceof Error).toBe(true);
    });
  });

  describe('when skip is true', () => {
    beforeAll(() => {
      mockHexo.config.theme_config.search = {
        skip: true
      };
      generate = generator({
        hexo: mockHexo
      });
    });

    test('should return an empty function', () => {
      const result = generate();
      expect(result).toBeUndefined();
    });
  });

  describe('when background is true', () => {
    beforeAll(() => {
      mockHexo.config.theme_config.search = {
        background: true
      };
    });

    test('should run task in background', () => {
      generate = generator({
        hexo: mockHexo
      });

      generate({pages: 'testPages'}, () => {});
      expect(mockSend).toHaveBeenCalled();
      expect(mockFork).toHaveBeenCalled();
    });
  });
});
