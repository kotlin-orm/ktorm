
$(document).ready(function () {
  var algoliaSettings = window.__INITIAL_STATE__.config.algolia;
  var isAlgoliaSettingsValid = algoliaSettings.appId && algoliaSettings.apiKey && algoliaSettings.indexName;

  if (!isAlgoliaSettingsValid) {
    window.console.error('Algolia Settings are invalid.');
    return;
  }

  var search = instantsearch({
    appId: algoliaSettings.appId,
    apiKey: algoliaSettings.apiKey,
    indexName: algoliaSettings.indexName,
    searchParameters: {
      facets: ['lang'],
      facetsRefinements: {
        lang: [window.__INITIAL_STATE__.page.lang]
      }
    },
    searchFunction: function (helper) {
      var searchInput = $('#doc-search-input');

      if (searchInput.val()) {
        $('#doc-search-results').show();
        $('#page-content').hide();
        helper.search();
      } else {
        $('#doc-search-results').hide();
        $('#page-content').show();
      }
    }
  });

  // Registering Widgets
  [
    instantsearch.widgets.searchBox({
      container: '#doc-search-input',
      placeholder: undefined
    }),

    instantsearch.widgets.hits({
      container: '#doc-search-hits',
      hitsPerPage: 1000,
      templates: {
        item: function (data) {
          var rawContent = data._highlightResult._contentTruncate.value;
          var content = rawContent
              .replace(/[\t\n\s]+/g, ' ')
              .replace(/</g, '&lt;')
              .replace(/>/g, '&gt;')
              .replace(/&lt;em&gt;/g, '<em>')
              .replace(/&lt;\/em&gt;/g, '</em>')
          var startIndex = Math.max(0, content.indexOf('<em>') - 50);

          var matchedCount = 0;
          for (var field in data._highlightResult) {
            var matchedWords = data._highlightResult[field].matchedWords;
            if (matchedWords !== undefined) {
              matchedCount += matchedWords.length;
            }
          }

          return (
            '<a ' + 
              'href="' + window.__INITIAL_STATE__.config.root + data.path + '"' +
              'class="doc-search-results__list__link">' +
              data.title +
            '</a>' +
            '<span class="doc-search-results__list__score-divider">|</span>' +
            '<span class="doc-search-results__list__score">' + matchedCount + ' matches</span>' +
            '<p>' + content.substr(startIndex, 120) + '...</p>'
          );
        },
        empty: function (data) {
          return (
            '<h1 className="doc-search-results__title">' +
              'No results for <span className="doc-search-results__title__query">"' + data.query + '"</span>' +
            '</h1>' +
            '<p>There are no results for "' + data.query + '". Why not <strong>try typing another keyword?</strong></p>'
          );
        }
      },
      cssClasses: {
        root: 'doc-search-results__list',
        item: 'doc-search-results__list__item'
      }
    }),

    instantsearch.widgets.stats({
      container: '#doc-search-stats',
      templates: {
        body: function (data) {
          return (
            '<h1 className="doc-search-results__title">' +
              data.nbHits + ' results for <span className="doc-search-results__title__query">"' + data.query + '"</span>' +
            '</h1>'
          );
        }
      }
    }),

    instantsearch.widgets.pagination({
      container: '#doc-search-pagination',
      scrollTo: false,
      showFirstLast: false,
      labels: {
        first: '<<',
        last: '>>',
        previous: '<',
        next: '>'
      },
      cssClasses: {
        root: 'pagination',
        item: 'pagination-item',
        link: 'page-number',
        active: 'current',
        disabled: 'disabled-item'
      }
    })
  ].forEach(search.addWidget, search);

  search.start();
});