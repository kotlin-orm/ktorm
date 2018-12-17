'use strict';
const stripIndent = require('strip-indent');
const hexoUtil = require('hexo-util');
const hexoHighlight = hexoUtil.highlight;


const {mergeWith, isArray, uniq} = require('lodash');

const arrayMergeCustomizer = (objValue, srcValue) => {
  if (isArray(objValue)) {
    return uniq(objValue.concat(srcValue));
  }
};

const deepMerge = (object, sources, customizer) => {
  const finalCustomizer = customizer || arrayMergeCustomizer;
  return mergeWith(object, sources, finalCustomizer);
};


const toSlug = (words) => {
  return encodeURIComponent(words.replace(/\s/g, '-').replace(/\./g, ''));
};

const highlight = ({code, lang}) => {
  const config = {
    gutter: false,
    firstLine: false
  };

  if (lang){
    config.lang = lang;
  } else {
    config.autoDetect = true;
  }

  code = stripIndent(code).trim();
  return hexoHighlight(code, config);
};

module.exports = {
  deepMerge,
  toSlug,
  highlight
};
