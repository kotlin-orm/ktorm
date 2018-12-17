'use strict';

const DEFAULT_CONFIG =  {
  link_url: '',
  link_text: 'Contact Us',
  text: 'Didn\'t you find what are you looking for? <br /> Try searching again on the left menu or',
  navigation: true,
  navigation_label: 'SUPPORT'
};

const filter = (locals) => {
  if (locals.page.support === false) {
    return locals;
  }
  if (locals.config.theme_config.support) {
    locals.config.theme_config.support = Object.assign({}, DEFAULT_CONFIG, locals.config.theme_config.support);
    locals.page.support = Object.assign(
      {},
      locals.config.theme_config.support,
      locals.page.support || {}
    );
  }
  return locals;
};

module.exports = {DEFAULT_CONFIG, filter};
