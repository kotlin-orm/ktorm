'use strict';
const { highlight } = require('../../helpers');


const responseSample = (ctx) => {
  let sample = ctx.sample;
  if (sample && sample['application/json']){
    sample = sample['application/json'];
  }
  if (sample){
    const code = JSON.stringify(sample, null, 2);
    sample = highlight({
      code,
      lang: 'json'
    });
  }

  return {
    sample
  };
};


module.exports = responseSample;
