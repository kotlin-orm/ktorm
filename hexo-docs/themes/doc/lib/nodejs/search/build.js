'use strict';

const lunr = require('lunr');
const striptags = require('striptags');
const cheerio = require('cheerio');
const path = require('path');

/**
 * Build lunr search index and store
 *
 * Iterates over "registered" Indexers to get documents
 * to add to the search index and a store
 *
 * @param ctx - context
 * @return {{index, store}}
 */
module.exports = function build (ctx) {
  const logger = ctx.logger;
  const hrstart = process.hrtime();
  const INDEXERS = [PagesIndexer];

  const { documents, store, items } = INDEXERS.reduce((acc, indexer) => {
    const { documents, store, items } = indexer.build(ctx);
    acc.store = Object.assign({}, acc.store, store);
    acc.documents = acc.documents.concat(documents);
    acc.items = acc.items.concat(items);
    return acc;
  }, {
    documents: [], // all documents that should be added to search index
    items: [],     // all processed items
    store: {}      // merged store
  });

  logger.debug(`Building search index and store for ${items.length} items, mapping to ${documents.length} documents`);

  const index = lunr(function () {
    this.ref('id');
    this.field('title');
    this.field('body');
    documents.forEach(function (doc) {
      this.add(doc);
    }, this);
  });
  const hrend = process.hrtime(hrstart);
  logger.debug(`Search index was built in ${hrend[0]}s, ${(hrend[1] / 1000000).toFixed()}ms`);

  return {
    index,
    store,

  };
};

function sanitize (html) {
  return striptags(html, [], ' ').trim() // strip html tags
    .replace(new RegExp('\n', 'g'), '') /* removes linebreaks   */ // eslint-disable-line no-control-regex
    .replace(/ +(?= )/g,'') // removes double spaces
    .replace(/&#123;|&quot;|&#125;|&lt;|&gt;/g, '');
}

function urlFor (rootPath, pagePath) {
  return path.join(rootPath || '/', pagePath);
}


/**
 * PagesIndexer
 *
 * @private
 * @type {{filter: Function, toData: Function}}
 */
const PagesIndexer = {
  build: (ctx) => {
    const pages = Array.isArray(ctx.pages) ? ctx.pages : (ctx.pages && ctx.pages.data);
    const store = {};

    if (!Array.isArray(pages)) {
      throw new Error('Cannot find "pages" in the current context');
    }

    const items = pages.filter((page) => {
      const ext = page.source.split('.').pop().toLowerCase();
      return page.path
        && page.content
        && page.search !== 'exclude'
        && (ext === 'md' || ext === 'markdown' || ext === 'mdown');
    });

    const only = items.find((page) => {
      return page.search === 'only';
    });

    if (only) {
      items.length = 0;
      items.push(only);
    }

    const documents = items.reduce((acc, page) => {
      acc.push({
        id: page.path,
        title: page.title,
        body: sanitize(page.content),
        path: urlFor(ctx.rootPath, page.path)
      });

      const $ = cheerio.load(page.content);
      $('h1,h2').each(function () {
        const elem = $(this);
        const anchorPagePath = `${page.path}#${elem.attr('id')}`;

        let body = [];

        elem.nextUntil('h2').each(function () {
          body.push($(this).html());
        });

        body = body.join(' ');

        acc.push({
          id: `${page.path}#${elem.attr('id')}`,
          title: sanitize(elem.html()),
          body: sanitize(body),
          path: urlFor(ctx.rootPath, anchorPagePath)
        });
      });
      return acc;
    }, []);

    documents.forEach((doc) => {
      store[doc.id] = {
        title: doc.title,
        path: doc.path,
        body: doc.body
      };
    });
    return { documents, store, items };
  }
};
