'use strict';
const path = require('path');

module.exports = {
  mode: 'production',
  externals: {
    jquery: '$'
  },
  entry:  {
    'doc': './lib/browser/index.js'
  },
  output: {
    path: path.resolve(__dirname, 'source/script'),
    filename: '[name].js'
  },
  module: {
    rules: [
      {
        test: /.jsx?$/,
        loader: 'babel-loader',
        exclude: /node_modules/,
        query: {
          presets: ['es2015', 'react']
        }
      }
    ]
  }
};
