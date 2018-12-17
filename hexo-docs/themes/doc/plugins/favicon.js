'use strict';

const util = require('../lib/nodejs/hexo-util');

module.exports = ({hexo}) => {
  const {themeConfig} = util({hexo});

  themeConfig({ favicon: '/favicon.ico' });
};
