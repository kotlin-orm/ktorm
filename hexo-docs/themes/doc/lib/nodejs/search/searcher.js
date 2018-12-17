'use strict';

const escapeStringRegexp = require('escape-string-regexp');

module.exports = function searcher ({index, store}) {
  return function search (query) {
    let matches = index.search(query); // try search the exact keyword

    if (matches.length === 0) { // if no results, try with * "magic"
      matches = matches.concat(index.search(query + '*'));
    }

    return matches.reduce((results, match) => {
      // transform search match entries into actual results
      // by reconnecting them to store entry and enhance with useful properties
      // from search results
      if (store[match.ref]) {
        results.push(Object.assign({}, store[match.ref], {
          ref: match.ref,
          score: match.score,
          matchMetadata: match.matchData.metadata
        }));
      }
      return results;
    }, []).map((result) => {
      // enhance entry with highlight property representing
      // the actual matched tokens and the fields where those where found
      const highlight = Object.keys(result.matchMetadata).map((text) => {
        const fields = Object.keys(result.matchMetadata[text]);
        return { text, fields };
      });
      return Object.assign({}, result, { highlight });

    }).map((entry) => {

      // truncate `body` where "center" is the first matched text to highlight
      if (entry.body && entry.highlight.length) {
        const text = entry.highlight[0].text;
        const firstHighlightIndex = entry.body.indexOf(text);
        entry.body = '...' + entry.body.substring(firstHighlightIndex - 100, firstHighlightIndex + 200) + '...';
      }

      // add highlight markup
      entry.highlight.forEach((h) => {
        h.fields.forEach((f) => {
          if (f === 'body') {
            entry[f] = entry[f].replace(new RegExp(escapeStringRegexp(h.text), 'gi'), `<span class="doc-highlight">${h.text}</span>`);
          }
        });
      });
      return entry;
    });
  };
};
