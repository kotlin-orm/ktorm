'use strict';

const util = require('./hexo-util');

module.exports = ({hexo}) => {
  const {themeConfig} = util({hexo});

  themeConfig({ favicon: '/favicon.ico' });
};
