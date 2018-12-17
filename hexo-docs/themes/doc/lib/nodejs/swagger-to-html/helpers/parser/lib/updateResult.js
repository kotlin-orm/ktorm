'use strict';

const updateResult = ({object, key, isRequired, result}) => {
  if (!key){
    return false;
  }

  result[key] = result[key] || {};

  const _result = result[key];

  const description = object.description;
  const type = object.type;
  const values = object.values;    // Used for enums
  const required = object.required || isRequired;
  const example = object.example;
  const format = object.format;

  if (description){
    _result.description = description;
  }

  if (type){
    _result.type = type;
  }

  if (values){
    _result.values = values;
  }

  if (typeof required === 'boolean' && required){
    _result.required = required;
  }

  if (example){
    _result.example = example;
  }

  if (format){
    _result.format = format;
  }

  return true;
};

module.exports = {updateResult};
