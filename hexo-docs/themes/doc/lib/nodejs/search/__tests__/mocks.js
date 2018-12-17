'use strict';

const mockLogger = {
  info: () => {},
  error: () => {},
  debug: () => {}
};

const mockHexo = {
  env: {},
  config: {
    theme_config: {
      search: {
        route: 'testlunr.json'
      }
    }
  },
  route: {
    'set': () => {}
  },
  log: mockLogger
};


const mockCb = () => {};

const mockHexoUtil = () => {
  return {
    url_for: (path) => {
      return path;
    },
    themeConfig: () => {
      return mockHexo.theme_config;
    }
  };
};

module.exports = {
  mockHexo,
  mockLogger,
  mockCb,
  mockHexoUtil
};
