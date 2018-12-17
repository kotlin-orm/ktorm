'use strict';

const {filter} = require('../lib/nodejs/support');

module.exports = ({hexo}) => {
  hexo.extend.filter.register('template_locals', filter);
};
