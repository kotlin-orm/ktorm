'use strict';

const searcher = require('../searcher');
const build = require('../build');
const {mockLogger} = require('./mocks');

describe('search.searcher', () => {
  let search;

  beforeAll(() => {
    const {index, store} = build({
      logger: mockLogger,
      pages: [
        {
          title: 'sed-cursus',
          content: 'Sed cursus nisl a interdum cursus. Sed consequat mi sit amet nulla molestie, vel cursus urna maximus. Etiam ac est ut libero condimentum eleifend quis feugiat urna. Vivamus consectetur odio diam, at faucibus lacus auctor sed. Nunc laoreet tellus id congue lobortis. Etiam et convallis velit. Donec a mauris quis ligula dignissim feugiat non in urna. In augue turpis, varius nec lacus sit amet, dictum sodales ante. Morbi volutpat eget libero ut porttitor.',
          path: 'sed',
          source: 'sed.md'
        },
        {
          title: 'lorem',
          content: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed rhoncus elit libero, eget sollicitudin elit dignissim quis. Morbi ornare risus quis augue bibendum fringilla. Pellentesque pharetra pellentesque elit sed lobortis. Etiam dignissim, orci non convallis porta, erat nibh sodales nibh, et tempor leo diam ut lorem. Phasellus ullamcorper euismod dui, sit amet aliquam urna vestibulum accumsan. Integer ullamcorper cursus placerat. Aenean sodales, odio et efficitur mollis, sem nibh iaculis lacus, vel blandit nunc libero id risus. Ut ultrices diam ut magna porta, non molestie augue porttitor. Cras nec leo facilisis, lacinia risus quis, aliquet purus. In viverra et ligula eget ultrices. Integer ac tincidunt magna.',
          path: 'lorem',
          source: 'lorem.md'
        }
      ]
    });
    search = searcher({ index, store });
  });

  it('should return a `search` function', () => {
    expect(typeof search).toBe('function');
  });

  describe('search', () => {
    it('should return expected results in the right order', () => {
      const entries = search('sed');
      expect(entries.length).toBeGreaterThanOrEqual(2);
      expect(entries[0].ref).toBe('sed'); // has higher score since the search query was found also in the title
      expect(entries[1].ref).toBe('lorem');
    });

    it('should highlight and truncate the body according to the search query', () => {
      const entries = search('sed');
      expect(entries[0].body.indexOf('<span class="doc-highlight">sed</span>') > - 1).toBe(true);
    });
  });

});
