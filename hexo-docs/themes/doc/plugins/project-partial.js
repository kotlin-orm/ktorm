'use strict';

const {createHelper} = require('../lib/nodejs/project-partial');

module.exports = ({hexo}) => {

  hexo.extend.helper.register('project_partial', createHelper({
    theme_config: hexo.config.theme_config,
    source_dir: hexo.source_dir,
    render: hexo.render,
    log: hexo.log
  }));

};
