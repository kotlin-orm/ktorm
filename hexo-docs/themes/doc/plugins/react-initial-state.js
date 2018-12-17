'use strict';

module.exports = ({hexo}) => {
  const allowedProperties = {
    config: [
      'root',
      'theme',
      'theme_config',
      'time_format',
      'timezone'
    ],
    page: [
      'path',
      'title',
      'support'
    ]
  };

  const filter = (source, allowedValues) => {
    return Object
      .keys(source)
      .filter(key => allowedValues.includes(key))
      .reduce((obj, key) => {
        obj[key] = source[key];
        return obj;
      }, {});
  };

  hexo.extend.filter.register('template_locals', function (locals){

    const data = locals.site.data;

    const page = filter(locals.page, allowedProperties.page);

    const config = filter(locals.config, allowedProperties.config);

    locals.initial_state = {
      page,
      data,
      config
    };

    return locals;
  }, 20);
};
