'use strict';

/**
 * Index posts on Algolia
 * @param {Object} args
 * @param {Function} callback
 * @returns {void}
 */
function algolia(args, callback) {
  var _ = require('lodash');
  var hexoUtil = require('hexo-util');
  var algoliasearch = require('algoliasearch');
  var async = require('async');
  var hexo = this;
  var algoliaConfig = hexo.config.algolia;
  var fields = getBasicFields(algoliaConfig.fields);
  var fieldsWithFilters = getFieldsWithFilters(algoliaConfig.fields);
  // Get user defined variables from env variables OR _config.yml
  // Note: That we will ignore any information set into _config.yml if the user setted the env variables.
  var appId = process.env.ALGOLIA_APP_ID || algoliaConfig.appId;
  var adminApiKey = process.env.ALGOLIA_ADMIN_API_KEY || algoliaConfig.adminApiKey;
  var indexName = process.env.ALGOLIA_INDEX_NAME || algoliaConfig.indexName;
  var log = hexo.log;
  var filters = {
    strip: hexoUtil.stripHTML,
    truncate: function(post, start, end) {
      return post.substr(start, end);
    }
  };

  /**
   * Process data of a post
   * @param {Object} post A post of Hexo
   * @returns {Object} post A post of Hexo
   */
  function processPost(post) {
    // index only published posts
    var object = _.pick(post, fields);
    // define objectID for Algolia
    object.objectID = post._id;

    // execute filters of fields
    fieldsWithFilters.forEach(function(field) {
      var indexedFieldName = [];
      var _filters = field.split(':');
      var fieldName = _filters.shift();

      if (!post.hasOwnProperty(fieldName)) {
        log.info('"' + post.title + '" post has no "' + fieldName + '" field.');
        return;
      }

      var fieldValue = post[fieldName];

      _filters.forEach(function(filter) {
        var filterArgs = filter.split(',');
        var filterName = filterArgs.shift();

        indexedFieldName.push(_.upperFirst(filterName));
        filterArgs.unshift(fieldValue);
        // execute filter on field value
        fieldValue = filters[filterName].apply(this, filterArgs);
      });

      // store filter result in post object
      object[fieldName + indexedFieldName.join('')] = fieldValue;
    });

    return object;
  }

  /**
   * Index posts
   * @param {String} index - An index name
   * @param {Array} posts - A list of posts
   * @returns {void}
   */
  function indexPosts(index, posts) {
    // split our results into chunks of 5,000 objects,
    // to get a good indexing/insert performance
    var chunkedPosts = _.chunk(posts, algoliaConfig.chunkSize || 5000);

    log.info('Indexing posts on Algolia...');
    async.each(chunkedPosts, index.saveObjects.bind(index), function(err) {
      if (err) {
        log.info('Error has occurred during indexing posts : ' + err);
        throw err;
      }
      log.info('Indexation done. ' + posts.length + ' posts indexed.');
    });
  }

  // start process with a generate call
  hexo.call('generate', function(err) {
    if (err) {
      log.info('Error has occurred during generating posts : ' + err);
      return callback(err);
    }

    var posts = [];
    var hexoPages = hexo.database.model('Page').find({});

    for (var i = 0; i < hexoPages.length; i++) {
      posts.push(processPost(hexoPages.data[i]));
    }

    log.info(posts.length + ' posts collected');
    // init index
    var client = algoliasearch(appId, adminApiKey);
    var index = client.initIndex(indexName);

    // clear index
    if (args && !args.n) {
      log.info('Clearing index on Algolia...');
      index.clearIndex(function(err) {
        if (err) {
          log.info('Error has occurred during clearing index : ' + err);
          return err;
        }
        log.info('Index cleared');
        indexPosts(index, posts);
      });
    }
    else {
      indexPosts(index, posts);
    }
  });
}

/**
 * Get fields without filters from a list of fields
 * @param {Array} fields - A list of fields. E.g: content, excerpt, categories, etc...
 * @returns {Array} - A list of fields without any filters
 */
function getBasicFields(fields) {
  return fields.filter(function(field) {
    return !/:/.test(field);
  });
}

/**
 * Get fields with filters
 * @param {Array} fields - A list of fields. E.g: content, excerpt, categories, etc...
 * @returns {Array} - A list of fields with filters
 */
function getFieldsWithFilters(fields) {
  return fields.filter(function(field) {
    return /:/.test(field);
  });
}

// register `hexo algolia` command
hexo.extend.console.register('algolia', 'Index your posts on Algolia', {
  options: [{
    name: '-n, --no-clear', desc: 'Does not clear the existing index'
  }]
}, algolia);
