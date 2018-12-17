'use strict';

const modules = require('./modules');
const {createTransformer} = require('./core');

module.exports = ({hexo}) => createTransformer({
  input: (api) => {
    const swaggerStore = require('../swagger-store')({hexo});
    return swaggerStore
      .getSwagger(api, true)
      .then(({swagger}) => {
        return Promise.resolve(swagger.swaggerObject);
      });
  },
  modules
});
