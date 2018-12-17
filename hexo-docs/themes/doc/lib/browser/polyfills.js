require('whatwg-fetch');
const Promise = require('promise-polyfill');
if (window && !window.Promise) { window.Promise = Promise; }
