'use strict';


const {traverse} = require('./traverse');

function init ({object}){
  return  traverse({object, key: '', init: true});
}

module.exports = {
  init
};
