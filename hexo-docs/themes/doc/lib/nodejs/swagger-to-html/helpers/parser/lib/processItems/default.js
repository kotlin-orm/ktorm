'use strict';

const {updateResult} = require('../updateResult');

const processDefault = ({object, key, isRequired, result}) => {
  updateResult({object, key, isRequired, result});
  return result;
};

module.exports = processDefault;
