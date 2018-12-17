'use strict';

const Hexo = require('hexo');
const hexo = new Hexo();
const hexoUtil = require('../hexo-util');
const mockRoute = 'mockRoute';
const mockUrlFor = jest.fn().mockImplementation(route => '/' + route);


hexo.config = {
  relative_link: 'relative_link'
};
hexo.extend.helper.store['url_for'] = mockUrlFor;

describe('hexo-util', () => {

  const {url_for, themeConfig} = hexoUtil({hexo});

  describe('url_for', () => {
    const url = url_for(mockRoute);
    test('should call native url_for method', () => {
      expect(url).toEqual('/' + mockRoute);
    });
  });

  describe('themeConfig', () => {

    it('should initialize `theme_config` if undefined',  () => {
      expect(hexo.config.theme_config).toBeUndefined();
      themeConfig();
      expect(hexo.config.theme_config).toBeDefined();
    });

    it('should return `theme_config` as the result of a deep merge with the object passed as an argument and the current `theme_config`',  () => {
      hexo.config.theme_config = {
        foo: {
          bar: 'hello'
        },
        john: 'doe'
      };
      const defaults = {
        foo: {
          bar: 'world'
        },
        search: true
      };

      const actual = themeConfig(defaults);
      const expected = {
        foo: {
          bar: 'hello'
        },
        john: 'doe',
        search: true,
      };

      expect(actual).toEqual(expected);
      expect(hexo.config.theme_config).toEqual(expected);
    });
  });
});
