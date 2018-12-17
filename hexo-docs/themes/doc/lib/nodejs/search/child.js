'use strict';

const childOnMessage = require('./child-on-message');

process.on('message', childOnMessage({process}));
