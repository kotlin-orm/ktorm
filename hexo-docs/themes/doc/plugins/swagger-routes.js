'use strict';

const getFilter = (hexo) => {

  const swaggerStore = require('../lib/nodejs/swagger-store')({hexo});

  return () => {
    const routes = swaggerStore.getRoutes();
    routes && Object
      .keys(routes)
      .forEach((route) => {
        const data = routes[route];

        if(data){
          hexo.route.set(route, data);
        }else{
          hexo.route.remove(route);
        }
      })
  }
}


module.exports = ({hexo}) => {
  const filter = getFilter(hexo);
  hexo.extend.filter.register('after_generate', filter);
};



