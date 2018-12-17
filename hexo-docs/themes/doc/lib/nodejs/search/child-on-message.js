'use strict';

const build = require('./build');
const createLogger = require('hexo-log');

module.exports = ({process}) => {

  return function (message) {
    const pages = message.pages ? Array.from(message.pages.data) : [];
    const logger = createLogger({ debug: message.debug });
    const rootPath = message.rootPath || '';
    const result = build({pages, logger, rootPath});
    process.send(result);
  };
};
