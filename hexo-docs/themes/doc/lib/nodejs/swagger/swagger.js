'use strict';

const SwaggerParser = require('swagger-parser');
const Promise = require('bluebird');
const jsYaml = require('js-yaml');
const { merge } = require('lodash');
const isPlainObj = require('is-plain-object');

class Swagger{

  /**
   * @param swagger File path for swagger schema or swagger object.
   * @params [options={}] swagger parser options
   */
  constructor (swaggerInput, options = {}){
    if (!swaggerInput){
      throw new TypeError('Please provide path for swagger schema or a valid swagger object.');
    } else {
      this.swaggerInput = swaggerInput;
      // Object containing merge of dereferenced swagger and swagger containing references.
      this.swaggerObject = null;
      this.options = options;
    }
  }


  /**
   * Validates the swagger schema and gives dereferenced swagger object.
   */
  validate () {
    const that = this;
    return this
      ._validate(this.swaggerInput)
      .then((dereferencedSchema) => {
        that.swaggerObject = dereferencedSchema;
        return that;
      });
  }


  /**
   * Validates the swagger schema and gives dereferenced swagger object.
   */
  bundle (){
    const that = this;
    return this
      ._bundle(this.swaggerInput)
      .then((referencedSchema) => {
        that.swaggerObject = referencedSchema;
        return that;
      });
  }


  /**
   * Decorates swagger object.
   */
  decorate (decorator){
    if ('function' === typeof decorator){
      this.swaggerObject = decorator(this.swaggerObject);
    }
    return Promise.resolve(this);
  }


  /**
   * Returns yamlSchema string.
   */
  get swaggerYaml (){
    // Stringify will flat the swaggerObject and resolve all the references
    // Passing the swagger object directly to yaml parser is creating &ref_0 in the generated YAML
    // FIXME: Check what is wrong with swaggerObject
    const jsonString = this.swaggerJson;
    const cleanObject = JSON.parse(jsonString);
    return jsYaml.safeDump(cleanObject);
  }


  /**
   * Returns jsonSchema string.
   */
  get swaggerJson (){
    return JSON.stringify(this.swaggerObject);
  }

  /**
   * Validates and bundles swagger.
   *
   * Merges dereferenced and referenced swagger objects, the merged object will contain $refs and the referenced object.
   */
  merge (){
    const validatePromise = this._validate(this.swaggerInput);
    const bundlePromise = this._bundle(this.swaggerInput);
    const promiseArray = [validatePromise, bundlePromise];
    const that = this;

    // Merging referenced and dereferenced swagger object to have ref links and the dereferenced object.
    return Promise.reduce(
      promiseArray,
      (aggregate, swagger) => {
        return merge(aggregate, swagger);
      },
      {}
    )
      .then((mergedSchema) => {
        that.swaggerObject = mergedSchema;
        return Promise.resolve(this);
      });
  }

  /**
   * Revert the merge of referenced and dereferenced swagger, and use $ref where-ever it is.
   *
   * */
  unmerge (dereferenced){
    const refMatchingFunction = (obj) => {
      if (obj.hasOwnProperty('$ref')){
        return true;
      }
      return false;
    };

    this.swaggerObject = this._traverseToUnmerge(this.swaggerObject, refMatchingFunction, dereferenced);

    const that = this;

    return this
      ._validate(this.swaggerObject)
      .then(() => {
        return that;
      })
      .catch((e) => {
        throw new SyntaxError(`Swagger Schema validation failed after un-merging swagger object.\n\nORIGINAL MESSAGE: \n${e.message}`);
      });
  }



  /**
   * Internal Methods.
   */

  /**
   * Validates the swagger schema and gives dereferenced swagger object.
   */
  _validate (schema){
    return SwaggerParser.validate(schema, this.options);
  }


  /**
   * Bundles and returns referenced swagger object.
   */
  _bundle (schema){
    return SwaggerParser.bundle(schema, this.options);
  }

  /**
   * Traverse swagger object and unmerge objects containing both $refs and the dereferenced object. Return referenced or dereferenced schema based on the value of @dereferenced.
   *
   * @subject: The object to traverse.
   * @fn: Function to check when to unmerge.
   * @dereferenced: Boolean value, if true returns derefrenced schema else referenced schema is returned.
   */
  _traverseToUnmerge (subject, fn, dereferenced){
    if (!fn){
      return subject;
    }
    if (isPlainObj(subject)){

      if (fn(subject)){
        if (dereferenced){
          delete subject['$ref'];
        } else {
          subject = {
            '$ref': subject['$ref']
          };
        }
      }

      subject = Object
        .keys(subject)
        .reduce((acc, key) => {
          if (fn(subject[key])){
            if (dereferenced){
              delete subject[key]['$ref'];
            } else {
              subject[key] = {
                '$ref': subject[key]['$ref']
              };
            }
          } else {
            subject[key] = this._traverseToUnmerge(subject[key], fn, dereferenced);
          }
          return acc;
        },
        subject);

    } else if (Array.isArray(subject)){
      subject = subject
        .reduce((acc, key) => {
          acc.push(this._traverseToUnmerge(key, fn, dereferenced));
          return acc;
        },
        []);
    }

    return subject;
  }
}

module.exports = Swagger;
