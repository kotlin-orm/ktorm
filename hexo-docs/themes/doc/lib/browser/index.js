require('./polyfills');

const React = require('react');
const ReactDOM = require('react-dom');
const {Navigation} = require('./navigation/containers.jsx');
const {SupportFooter} = require('./support/containers.jsx');
const {LangSwitcher} = require('./lang-switcher/components.jsx');

const props = Object.assign({}, window.__INITIAL_STATE__, {log: console});
const page = props.page;
const url_for = require('./utils').url_for(props);

ReactDOM.hydrate(
  React.createFactory(Navigation)(props),
  document.getElementById('react-navigation-root')
);

ReactDOM.render(
  React.createFactory(SupportFooter)(props),
  document.getElementById('react-support-footer-root')
);

ReactDOM.render(
  React.createFactory(LangSwitcher)({page, url_for}),
  document.getElementById('lang-switcher-container')
);