'use strict';

const {merge} = require('lodash');

module.exports = ({hexo}) => {
  return {
    /**
     * url_for helper that you can normally use in templates
     * but for programmatic usage in hexo plugins
     *
     * @type {function}
     */
    url_for: hexo.extend.helper.get('url_for').bind({
      config: hexo.config,
      relative_url: hexo.extend.helper.get('relative_url')
    }),
    /**
     * Get or merge theme_config
     *
     * @param  {Object} object - the object that you wanna merge
     * @return {Object} current theme_config
     */
    themeConfig: (object) => {
      if (!hexo.config.theme_config) {
        hexo.config.theme_config = {};
      }

      if (!object) {
        return hexo.config.theme_config;
      }

      hexo.config.theme_config = merge({}, object, hexo.config.theme_config);

      return hexo.config.theme_config;
    }
  };
};
