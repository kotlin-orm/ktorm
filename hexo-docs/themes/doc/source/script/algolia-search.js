
$(document).ready(function () {
  var lang = window.__INITIAL_STATE__.page.lang;
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
    // urlSync: true,
    searchParameters: {
      facets: ['lang'],
      facetsRefinements: {
        lang: [lang === 'zh-cn' ? 'zh-cn' : 'en']
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
      container: '#doc-search-input'
    }),

    instantsearch.widgets.hits({
      container: '#doc-search-hits',
      hitsPerPage: 1000,
      templates: {
        item: function (data) {
          var rawContent = data._highlightResult._contentTruncate.value;
          var startIndex = Math.max(0, rawContent.indexOf('<em>') - 50);

          var matchedCount = 0;
          for (var field in data._highlightResult) {
            var matchedWords = data._highlightResult[field].matchedWords;
            if (matchedWords !== undefined) {
              matchedCount += matchedWords.length;
            }
          }

          return (
            '<a' +
              ' href="' + window.__INITIAL_STATE__.config.root + data.path.replace('index.html', '') + '"' +
              ' class="doc-search-results__list__link">' +
              data.title +
            '</a>' +
            '<span class="doc-search-results__list__score-divider">|</span>' +
            '<span class="doc-search-results__list__score">' + matchedCount + ' matches</span>' +
            '<p>' + 
              rawContent.substr(startIndex, 300)
                .replace(/[\t\n\s`]+/g, ' ')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/&lt;em&gt;/g, '<em>')
                .replace(/&lt;\/em&gt;/g, '</em>') +
              '...' + 
            '</p>'
          );
        },
        empty: function (data) {
          if (lang === 'zh-cn') {
            return (
              '<h1 class="doc-search-results__title">' +
              '未找到与 <span class="doc-search-results__title__query">"' + data.query + '"</span> 相关的内容' +
              '</h1>' +
              '<p>未找到与 "' + data.query + '" 相关的内容，请 <strong>尝试其他关键字</strong></p>'
            );
          } else {
            return (
              '<h1 class="doc-search-results__title">' +
              'No results for <span class="doc-search-results__title__query">"' + data.query + '".</span>' +
              '</h1>' +
              '<p>There are no results for "' + data.query + '". Why not <strong>try typing another keyword?</strong></p>'
            );
          }
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
          if (lang === 'zh-cn') {
            return (
              '<h1 class="doc-search-results__title">' +
              '找到 ' + data.nbHits + ' 条结果，耗时 ' + data.processingTimeMS + ' 毫秒' +
              '</h1>'
            );
          } else {
            return (
              '<h1 class="doc-search-results__title">' +
              data.nbHits + ' results found in ' + data.processingTimeMS + 'ms.' +
              '</h1>'
            );
          }
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