'use strict';

const {updateResult} = require('../updateResult');

const processEnum = ({object, key, isRequired, result}) => {
  object.type = 'enum';
  object.values = object.enum;
  updateResult({object, key, isRequired, result});
  return result;
};

module.exports = processEnum;

