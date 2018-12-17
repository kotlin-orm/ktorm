'use strict';


const path = require('path');
const touch = require('touch');
const parseSchemaFile = require('./parse-schema-file.js');
const hexoUtil = require('../hexo-util');
const validUrl = require('valid-url');

const DEFAULT_CONFIG = {
  version: 2,

  // version 2 specifics
  permalinks: true,
  api_explorer: true,
  download: 'Download specification',

  // version 3 specifics
  show_extensions: false,
  deep_linking: true,
  display_operation_id: false,
  doc_expansion: 'none'
};

const createSwaggerUI = function ({hexo}) {
  const log = hexo.log || console;
  /**
   * processedPages = {}
   * processedPages is a dictionary to make sure that js/css library tags are inserted only once in the page.
   *
   * Structure:
   * {
   *    processedPageURI: 'true/false depending upon whether or not library tags are inserted in it or not.'
   * }
   * */
  const processedPages = {};

  /**
   * specBacklinks
   *
   * A data structure to store the files that are embedding a particular spec
   * so that when ever the spec changes we can touch the file that is using the spec(and this will force the file to reload as its last modified time got changed).
   *
   * Structure:
   * {
   *  pathOfSwaggerFile: Set('path/of/mdFile1', 'path/of/mdFile2')    //Set of files using the swagger file.
   * }
   *
   */
  const specBacklinks = {};

  const uiGenerator = (function () {

    /* id is an autoincrement variable to keep the track of instances, so that we can have unique HTML id in each instance */
    let id = 0;

    return {
      /*
       * Function exposed to get HTML for a specific swagger object.
       *
       * pageSource: It is used to make sure that the libraries are inserted only once.
       *
       * swagger: The swagger object.
       */
      getHtml: function ({pageSource, swagger, downloadRoute, options}) {
        id++;

        /**
         * url_for:
         * Helper to fix issue with render not understanding helpers. Injecting helpers to render.
         */
        const {url_for} = hexoUtil({hexo});
        const versionKey = `v${options.version}`;
        const partials = {
          libs: path.resolve(__dirname, `./partials/${versionKey}/libs.ejs`),
          snippet: path.resolve(__dirname, `./partials/${versionKey}/snippet.ejs`)
        };

        /**
         * Render the angular snippet.
         *
         * Check whether the page is in processedPages.
         *
         * If it is there just respond with the angular snippet and if not also add the library tags.
         *
         * And update processedPages if you process the page.
         */
        const snippetLocals = {
          id,
          swagger,
          swaggerUrl: url_for(downloadRoute),
          url_for,
          options
        };

        const libsLocals = { url_for };

        return hexo
          .render
          .render({path: partials.snippet}, snippetLocals)
          .then((snippet) => {
            if (processedPages[pageSource] && processedPages[pageSource][versionKey]) {
              return `${snippet}`;
            }

            return hexo
              .render
              .render({path: partials.libs}, libsLocals)
              .then((libs) => {
                return `${libs}${snippet}`;
              });
          })
          .then((html) => {
            processedPages[pageSource] = Object.assign(processedPages[pageSource] || {}, {
              [versionKey]: true
            });
            return `<div class="hexo-swagger-ui hexo-swagger-ui-${versionKey}">${html}</div>`;
          });
      }
    };
  })();


  function swaggerUITag (args, content){
    const ctx = this;
    const pageSource = ctx.source;
    let swaggerPath = args[0];
    let options = {};

    if (!validUrl.isUri(swaggerPath)){
      swaggerPath = path.resolve(path.dirname(ctx.full_source), swaggerPath);
    }

    if (content && typeof content === 'string') {
      try {
        options = JSON.parse(content);
      } catch (error) {
        log.error(error);
      }
    }

    options = Object.assign({}, hexo.config.theme_config.swagger_ui, options);

    /**
     * Add the current page to specBacklinks for current swagger file.
     */
    if (!specBacklinks[swaggerPath]){
      specBacklinks[swaggerPath] = new Set();
    }
    specBacklinks[swaggerPath].add(ctx.full_source);




    return parseSchemaFile(swaggerPath, pageSource, hexo)
      .then(({swagger, downloadRoute}) => {
        return uiGenerator.getHtml({pageSource, swagger, downloadRoute, options});
      })
      .catch(error => {
        log.error(error);
        if (error.filePath && error.referencePath) {
          log.error('File path:' + error.filePath);
          log.error('File is referenced in:' + error.referencePath);
          log.error('Skipping the file.');
        }
      });
  }

  function swaggerUIProcessor (file) {

    /**
     * Since the file is being reprocessed and will start the complete lifecycle from the beginning,
     * If the file is in processedPages reset the status of included libs
     * so that it can be processed by uiGenerator as a new unprocessed file.
     */
    if (processedPages[file.path]){
      processedPages[file.path] = {};
    }

    /**
     *  Since the function watches every change, it will capture changes for spec files as well.
     *  If the source(path of the file) of changed file is in specBacklinks we need to get all the pages for that file and modify their last updated time so that hexo reloads that file as well.
     */
    const files = specBacklinks[file.source];
    if (files){
      for (const value of files) {
        touch(value);
      }
    }
  }

  return {
    swaggerUITag,
    swaggerUIProcessor
  };
};

createSwaggerUI.DEFAULT_CONFIG = DEFAULT_CONFIG;

module.exports = createSwaggerUI;
