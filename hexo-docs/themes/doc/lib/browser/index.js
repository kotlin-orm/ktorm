require('./polyfills');

const React = require('react');
const ReactDOM = require('react-dom');
const {Navigation} = require('./navigation/containers.jsx');
const {SearchResults} = require('./search/containers.jsx');
const {SupportFooter} = require('./support/containers.jsx');
const PROPS = Object.assign({}, window.__INITIAL_STATE__, {log: console});

require('./swagger-to-html');

ReactDOM.hydrate(
  React.createFactory(Navigation)(PROPS),
  document.getElementById('react-navigation-root')
);

ReactDOM.render(
  React.createFactory(SearchResults)(PROPS),
  document.getElementById('react-search-results-root')
);

ReactDOM.render(
  React.createFactory(SupportFooter)(PROPS),
  document.getElementById('react-support-footer-root')
);
