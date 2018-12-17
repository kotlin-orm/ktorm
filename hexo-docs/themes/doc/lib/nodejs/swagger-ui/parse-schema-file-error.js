'use strict';

module.exports = function ParseSchemaFileError ({message, filePath, referencePath}) {
  Error.captureStackTrace(this, this.constructor);
  this.name = this.constructor.name;
  this.message = message;
  this.filePath = filePath;
  this.referencePath = referencePath;
};

require('util').inherits(module.exports, Error);
