'use strict';

const generator = require('../lib/nodejs/search/generator');
const util = require('../lib/nodejs/hexo-util');

const DEFAULT_CONFIG = { route: '/lunr.json' };

module.exports = ({hexo}) => {
  const {themeConfig} = util({hexo});
  hexo.extend.generator.register('search', createGeneratorFn({hexo, themeConfig}));
};

function createGeneratorFn ({hexo, themeConfig}) {
  const cmd = hexo.env.args._ && hexo.env.args._.length ? hexo.env.args._[0] : null;

  // hexo commands that should activate the generator
  const cmds = [
    'generate',
    'server',
    'deploy',
    'g',
    's',
    'd'
  ];

  // hexo commands that should activate the generator in background mode
  const bgCmds = [
    'server',
    's'
  ];

  const skip = cmds.indexOf(cmd) === -1 && typeof hexo.env.args._ !== 'undefined';
  const background = bgCmds.indexOf(cmd) > -1;

  themeConfig({ search: { skip, background, route: DEFAULT_CONFIG.route } });

  return generator({hexo});
}
