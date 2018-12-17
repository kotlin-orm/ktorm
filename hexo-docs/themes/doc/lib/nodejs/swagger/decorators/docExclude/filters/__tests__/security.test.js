'use strict';

const securityFilter = require('../security');

const dummySwagger = {
  securityDefinitions:{
    'oauth':{

    },
    'internalAuth':{
      'x-doc': {
        'excluded': true
      }
    },
    'foo': {
      'x-doc': {
        'excluded': true
      }
    }
  },
  security: [
    {
      'oauth':[]
    },
    {
      'internalAuth': []
    }
  ],
  paths: {
    '/pets': {
      'get': {
      },
      'post': {
        security: [
          {
            'foo': []
          }
        ]
      }
    }
  }
};

const expectedSwagger = {
  securityDefinitions:{
    'oauth':{

    }
  },
  security: [
    {
      'oauth':[]
    }
  ],
  paths: {
    '/pets': {
      'get': {
      },
      'post': {
        security: []
      }
    }
  }
};

describe('docExclude.parameters', () => {
  it('should filter parameters', () => {
    const updatedSwagger = securityFilter(dummySwagger);

    expect(updatedSwagger).toEqual(expectedSwagger);
  });
});

