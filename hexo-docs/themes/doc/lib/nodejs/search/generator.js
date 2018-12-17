'use strict';

const path = require('path');
const { fork } = require('child_process');
const build = require('./build');
const hexoUtil = require('../hexo-util');

/**
 * Return a function that represents the hexo generator.
 * When "hexo server" command is used we fork a child process to produce the search index in background since the task can be a time expensive operation.
 * When the child process it's done, it sends back a message containing the computed search index
 *
 * @param options.hexo - the hexo global instance
 * @return {Function} - hexo generator
 */
module.exports =  ({hexo}) => {
  const logger = hexo.log || console;
  const {url_for} = hexoUtil({hexo});

  function setRoute ({ hexo, result }) {
    const route = hexo.config.theme_config.search.route;
    const url = url_for(route);
    logger.debug(`New search index available at: ${url}`);
    hexo.route.set(route, JSON.stringify(result));
  }

  const {skip, background} = hexo.config.theme_config.search;

  if (skip) { return () => {}; }

  // build the index within the same process
  // wait for the task to complete
  if (!background) {
    return function (locals, cb) {
      logger.debug('Start building the search index');
      const result = build({pages: locals.pages, rootPath: hexo.config.root, logger});
      setRoute({hexo, result});
      cb();
      return result;
    };
  }

  if (background) {
    logger.debug('Open a background child process to build the search index');
    // use a child process to don't block the main process
    const child = fork(path.resolve(__dirname, './child.js'));
    child.on('message', (message) => {
      setRoute({ hexo, result: message });
    });

    return function (locals, cb) {
      logger.debug('Send a message to build the search index in background');
      child.send({
        pages: locals.pages,
        debug: hexo.env.debug,
        rootPath: hexo.config.root
      });
      cb();
    };
  }
};
