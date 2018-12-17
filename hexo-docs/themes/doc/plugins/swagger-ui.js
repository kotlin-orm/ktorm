'use strict';

const createSwaggerUI = require('../lib/nodejs/swagger-ui/index');
const util = require('../lib/nodejs/hexo-util');

module.exports = ({hexo}) => {
  const {themeConfig} = util({hexo});
  const {swaggerUITag, swaggerUIProcessor} = createSwaggerUI({hexo});

  themeConfig({ swagger_ui: createSwaggerUI.DEFAULT_CONFIG });

  hexo.extend.tag.register('swagger_ui', swaggerUITag, {async: true});
  hexo.extend.tag.register('swagger_ui_advanced', swaggerUITag, {async: true, ends: true});

  /**
   * This funtion is called when any file is processed.
   * It is automatically hooked to the watch task and is called if any file is modified.
   * */
  hexo.extend.processor.register('*', swaggerUIProcessor);
};
