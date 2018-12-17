'use strict';

const {merge} = require('lodash');
let result;

function traverse ({object, key, isRequired, init}){

  if (!object){
    return {};
  }
  // handle allOf property
  if (object.allOf){
    let mergedObj = {type: 'object'};
    for (const obj of object.allOf){
      mergedObj = merge(mergedObj, obj);
    }
    object = mergedObj;

    // delete allOf as its already used
    delete object.allOf;
  }

  // Check object type.
  const type = object.type;
  const hasProperties = !!object.properties;

  if (init){
    result = {};
  }

  if ('array' === type){
    return processArray({object, key, isRequired, result});
  }

  // Some schemas, object does not have a type but have properties
  if ('object' === type || hasProperties){
    return processObject({object, key, isRequired, result});
  }

  if (object.enum){
    return processEnum({object, key, isRequired, result}) ;
  }

  return processDefault({object, key, isRequired, result});
}



module.exports = {
  traverse
};

const {processArray, processObject, processEnum, processDefault} = require('./processItems') ;
