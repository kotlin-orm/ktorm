'use strict';

const {mockLogger} = require('./mocks');

describe('search.child-on-message', () => {

  jest.mock('../build', () => () => 'test');
  jest.mock('hexo-log', () =>  () => mockLogger);

  let childOnMessage, mockProcess;

  beforeEach(() => {
    mockProcess = {
      send: jest.fn()
    };
    childOnMessage = require('../child-on-message')({ process: mockProcess });
  });

  it('should send a message back to parent process', () => {
    const message = {
      pages: { data: [] },
      rootPath: '/'
    };
    childOnMessage(message);

    expect(mockProcess.send).toHaveBeenCalledWith('test');
  });

  it('should send a message back to parent process also when pages is undefined', () => {
    const message = {
      rootPath: '/'
    };
    childOnMessage(message);

    expect(mockProcess.send).toHaveBeenCalledWith('test');
  });
});
