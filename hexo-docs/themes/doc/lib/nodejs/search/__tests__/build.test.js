'use strict';

const build = require('../build');
const {mockLogger} = require('./mocks');

describe('search.build', () => {

  it('should throw an error when `ctx.pages` is undefined', () => {
    expect(() => {
      build({logger: mockLogger});
    }).toThrow(new Error('Cannot find "pages" in the current context'));
  });

  it('should create the expected search index and expected store', () => {
    const result = build({
      logger: mockLogger,
      pages: [
        {title: 'foo', content: '<h2 id="foo"> Foo </h2>', path: '/foo', source: 'foo.md'},
        {title: 'bar', content: 'bar', path: '/bar', source: 'bar.md'}
      ]
    });

    expect(Object.keys(result.store).length).toBe(3);
  });


  it('should also use `ctx.pages.data` to support child process message', () => {
    const result = build({
      logger: mockLogger,
      pages: {
        data: [
          {title: 'foo', content: '<h2 id="foo"> Foo </h2> <p>Body</p> <h2 id="foo-2"> Foo2 </h2>', path: '/foo', source: 'foo.md'},
          {title: 'bar', content: 'bar', path: '/bar', source: 'bar.md'}
        ]
      }
    });

    expect(Object.keys(result.store).length).toBe(4);
  });

  it('should process "mardown" files and skip the others', () => {
    const result = build({
      logger: mockLogger,
      pages: {
        data: [
          {title: 'foo', content: 'foo', path: '/foo', source: 'foo.md'},
          {title: 'bar', content: 'bar', path: '/bar', source: 'bar.markdown'},
          {title: 'john', content: 'john', path: '/john', source: 'john.mdown'},
          {title: 'exe', content: 'exe', path: '/exe', source: 'exe.exe'},
        ]
      }
    });

    expect(Object.keys(result.store).length).toBe(3);
  });
});
