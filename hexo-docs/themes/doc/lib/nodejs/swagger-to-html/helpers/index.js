'use strict';


const { parseResponse, parseBody } = require('./parser');
const { toSlug , deepMerge, highlight } = require('./utils');

module.exports = {
  parseResponse,
  parseBody,
  toSlug,
  deepMerge,
  highlight
};

