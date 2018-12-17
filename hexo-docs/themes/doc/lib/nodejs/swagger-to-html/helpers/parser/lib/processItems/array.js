'use strict';

const {traverse} = require('../traverse');
const {updateResult} = require('../updateResult');

const processArray = ({object, key, isRequired, result}) => {
  updateResult({object, key, isRequired, result});
  const items = object.items;
  return traverse({
    object: items,
    key: key + '[]'
  });
};

module.exports = processArray;
