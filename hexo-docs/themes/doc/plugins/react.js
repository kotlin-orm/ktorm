'use strict';

// NOTE: "node-jsx" package is deprecated but it's only one
// that is working correctly without going crazy with presets and babel
// the correct solution should be:
//
// ```
// require('babel-register')({
//  'presets': ['react', 'es2015']
// });
// ```
//
// But for "yet" unknown reasons it works just when you `npm link` the package but not when
// you install it in a project with the usual `npm install`... ¯\_(ツ)_/¯
//


require('node-jsx').install();

const React = require('react');
const ReactDOM = require('react-dom/server');
const {Navigation} = require('../lib/browser/navigation/containers.jsx');
const components = {
  Navigation
};

module.exports = ({hexo}) => {

  /**
   * "Server-side render" a React component
   * @param  {String} componentName - The componentName
   * @param  {Object} [props={}] - injected props
   * @return {string}
   */
  function reactComponent (componentName, props = {}) {
    const Component = components[componentName];
    const componentFactory = React.createFactory(Component);
    return ReactDOM.renderToString(componentFactory(props));
  }

  hexo.extend.helper.register('react_component', reactComponent);
};
