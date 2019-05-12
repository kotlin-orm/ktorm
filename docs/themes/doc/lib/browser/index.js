const React = require('react');
const ReactDOM = require('react-dom');
const urljoin = require('url-join');
const {Navigation} = require('./navigation/containers.jsx');

const props = Object.assign({}, window.__INITIAL_STATE__, {log: console});

props.url_for = function (path) {
  if (/^(f|ht)tps?:\/\//i.test(path)) {
    return path;
  }

  const url = urljoin(props.config.root, path);

  // removes double slashes
  return url.replace(/\/{2,}/g, '/');
};

ReactDOM.hydrate(
  React.createFactory(Navigation)(props),
  document.getElementById('navigation-container')
);