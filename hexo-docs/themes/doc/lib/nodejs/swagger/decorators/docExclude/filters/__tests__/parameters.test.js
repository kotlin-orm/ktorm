'use strict';

const parametersFilter = require('../parameters');

const dummySwagger = {
  parameters: {
    'foo':{
      'name': 'foo'
    },
    'bar':{
      'name': 'bar',
      'x-doc': {
        'excluded': true
      }
    }
  },
  paths: {
    '/pets': {
      'get': {
        parameters:[
          {
            name: 'param1'
          },
          {
            name: 'param2',
            'x-doc':{
              'excluded': true
            }
          }
        ]
      }
    }
  }
};

const expectedSwagger = {
  parameters: {
    'foo':{
      'name': 'foo'
    }
  },
  paths: {
    '/pets': {
      'get': {
        parameters:[
          {
            'name': 'param1'
          }
        ]
      }
    }
  }
};

describe('docExclude.parameters', () => {
  it('should filter parameters', () => {
    const updatedSwagger = parametersFilter(dummySwagger);

    expect(updatedSwagger).toEqual(expectedSwagger);
  });
});

