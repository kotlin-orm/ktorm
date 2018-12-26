'use strict';

const DEFAULT_CONFIG =  {
  link_url: '',
  link_text: 'Contact Us',
  text: 'Didn\'t you find what are you looking for? <br /> Try searching again on the left menu or',
  navigation: true,
  navigation_label: 'SUPPORT'
};

module.exports = ({hexo}) => {
  hexo.extend.filter.register('template_locals', function(locals) {
    if (locals.page.support === false) {
      return locals;
    }

    const theme_config = locals.config.theme_config || {};

    if (theme_config.support) {
      theme_config.support = Object.assign({}, DEFAULT_CONFIG, theme_config.support);
      locals.page.support = Object.assign({}, theme_config.support, locals.page.support || {});
    }

    return locals;
  });
};
