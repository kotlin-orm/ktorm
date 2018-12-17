const React = require('react');
const {shallow, mount} = require('enzyme');

const {HIDE_SEARCH_RESULTS, SHOW_SEARCH_RESULTS} = require('../actions');

describe('browser.search.components', () => {

  const mockDispatch = jest.fn();

  jest.mock('../../utils', () => ({ dispatch: mockDispatch }));

  afterEach(() => {
    mockDispatch.mockClear();
  });

  describe('SearchForm', () => {
    const {SearchForm} = require('../components.jsx');

    it('should not render anything if search props is null', () => {
      const searchForm = shallow(<SearchForm search={null} />);
      expect(searchForm.getElement()).toEqual(null);
    });

    it('should render the input if search props is a function', () => {
      const searchForm = shallow(<SearchForm search={() => {}} />);
      expect(searchForm.find('input').length).toEqual(1);
    });

    it('should hide search results if search query is empty', () => {
      const searchForm = shallow(<SearchForm search={() => {}} />);
      searchForm.find('input').simulate('keyup', { target: { value: '' } } );
      expect(mockDispatch).toHaveBeenCalledWith(HIDE_SEARCH_RESULTS);
    });

    it('should return immediately if query is too short', () => {
      const searchForm = shallow(<SearchForm search={() => []} />);
      searchForm.find('input').simulate('keyup', { target: { value: 'fo' } } );
      expect(mockDispatch).not.toHaveBeenCalled();
    });

    it('should show results if search query is not empty or too short', () => {
      const searchForm = shallow(<SearchForm search={() => []} />);
      searchForm.find('input').simulate('keyup', { target: { value: 'foobar' } } );
      expect(mockDispatch).toHaveBeenCalledWith(SHOW_SEARCH_RESULTS, expect.any(Object));
    });


    it('should call onSearch callback if it\'s a function', () => {
      const mockOnSearch = jest.fn();
      const searchForm = shallow(<SearchForm search={() => []} onSearch={mockOnSearch} />);
      searchForm.find('input').simulate('keyup', { target: { value: 'foobar' } } );
      expect(mockOnSearch).toHaveBeenCalled();
    });
  });

  describe('SearchResultsTitle', () => {
    const {SearchResultsTitle} = require('../components.jsx');
    it('should shallow render without any error', () => {
      const title = shallow(<SearchResultsTitle results={[]} />);
      expect(title.length).toEqual(1);
    });
  });

  describe('SearchResultsList', () => {
    const {SearchResultsList} = require('../components.jsx');

    it('should not render anything if no results', () => {
      const list = shallow(<SearchResultsList results={[]} />);
      expect(list.getElement()).toEqual(null);
    });

    it('should show the expected number of results', () => {
      const results = [{
        score: 0.4,
        title: 'foobar',
        body: 'foobar'
      }];
      const list = shallow(<SearchResultsList results={results} />);
      expect(list.find('ul').children('li').length).toBe(1);
    });

    it('should render result.body html value', () => {
      const results = [{
        score: 0.4,
        title: 'foobar',
        body: '<div>foobar</div>'
      }];
      const list = mount(<SearchResultsList results={results} />);
      expect(list.find('ul').children('li').find('p').html()).toBe('<p><div>foobar</div></p>');
    });

    it('should hide results when a link is clicked', () => {
      const results = [{
        score: 0.4,
        title: 'foobar',
        body: 'foobar'
      }];
      const list = shallow(<SearchResultsList results={results} />);

      list.find('ul').children('li').find('a').last().simulate('click');

      expect(mockDispatch).toHaveBeenCalledWith(HIDE_SEARCH_RESULTS);
    });
  });
});
