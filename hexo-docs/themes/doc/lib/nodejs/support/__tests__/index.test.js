'use strict';

const {merge} = require('lodash');

describe('support', () => {
  const {filter, DEFAULT_CONFIG} = require('../index');
  describe('filter', () => {
    it('when font-matter page.support property is equal to false, it should leave untouched the locals object', () => {
      const locals = {
        page: { support: false }
      };
      const actual = filter(locals);

      expect(Object.keys(actual)).toEqual(Object.keys(locals));
      expect(actual.page.support).toEqual(locals.page.support);
    });

    it('when theme_config.support property is equal to a falsy value or undefined, it should leave untouched the locals object', () => {
      const locals = {
        page: { },
        config: {
          theme_config: { }
        }
      };
      const actual = filter(locals);

      expect(Object.keys(actual)).toEqual(Object.keys(locals));
      expect(actual.page.support).toEqual(locals.page.support);
    });

    it('should merge defaults to user provided support config', () => {
      const locals = {
        page: { },
        config: {
          theme_config: {
            support: {
              'link_text': 'Hello'
            }
          }
        }
      };
      const actual = filter(locals);
      const expected = merge({}, DEFAULT_CONFIG, { link_text: 'Hello'});

      expect(actual.page.support).toEqual(expected);
      expect(actual.config.theme_config.support).toEqual(expected);
    });
  });
});
