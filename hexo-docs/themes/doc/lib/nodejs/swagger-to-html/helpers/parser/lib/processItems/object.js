'use strict';

const {traverse} = require('../traverse');
const {updateResult} = require('../updateResult');

const processObject = ({object, key, isRequired, result}) => {
  updateResult({object, key, isRequired, result});

  // Check for additional properteis
  if (object.additionalProperties){
    object = object.additionalProperties;
  }

  const properties = object.properties || {};
  const required = object.required || [] ;

  return Object.keys(properties).reduce((result, propertyName) => {
    const property = properties[propertyName];
    const childKey = key ? (key + '.' + propertyName) : propertyName;
    const isRequired = required.includes(propertyName);

    return traverse({
      object: property,
      key: childKey,
      isRequired
    });

  }, result);
};

module.exports = processObject;
