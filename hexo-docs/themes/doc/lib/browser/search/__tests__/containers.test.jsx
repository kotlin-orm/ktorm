const React = require('react');
const {shallow, mount} = require('enzyme');
const {dispatch} = require('../../utils');

const {HIDE_SEARCH_RESULTS, SHOW_SEARCH_RESULTS} = require('../actions');

describe('browser.search.containers', () => {

  beforeEach(() => {
    document.documentElement.innerHTML = `
      <div id="page-content"></div>
    `;
  });

  describe('SearchResults', () => {
    const {SearchResults} = require('../containers');

    it('in initial state it should not be visible', () => {
      const results = shallow(<SearchResults />);
      expect(results.getElement()).toBe(null);
    });

    it('when SHOW_SEARCH_RESULTS action is triggered, should be visible', () => {
      const results = mount(<SearchResults />);
      dispatch(SHOW_SEARCH_RESULTS, { results: [] });
      expect(results.getElement()).not.toBe(null);
    });

    it('when SHOW_SEARCH_RESULTS action is triggered, it should hide page content', () => {
      const results = mount(<SearchResults />);
      expect(results.instance().$page.css('display')).toBe('block');
      dispatch(SHOW_SEARCH_RESULTS, { results: [] });
      expect(results.instance().$page.css('display')).toBe('none');
    });

    it('when HIDE_SEARCH_RESULTS action is triggered, it should not be visible and the page content is visible', () => {
      const results = mount(<SearchResults />);
      dispatch(SHOW_SEARCH_RESULTS, { results: [] });
      expect(results.instance().$page.css('display')).toBe('none');

      dispatch(HIDE_SEARCH_RESULTS);
      expect(results.instance().$page.css('display')).toBe('block');
      expect(results.find('div').length).toBe(0);
    });
  });
});
