'use strict';

const path = require('path');
const fs = require('fs');
const { Promise } = require('bluebird');
const validUrl = require('valid-url');

module.exports = ({hexo}) => {

  const { url_for } = require('../lib/nodejs/hexo-util')({hexo});
  const {getRoute, getDigest, prepareRoute} = require('../lib/nodejs/swagger-store')({hexo});

  class SwaggerProcessor{

    /*
     * Default templating enging is 'html'.
     */

    constructor (engine = 'html'){
      const availableEngines = ['md', 'html'];
      this.engine = null;

      if (availableEngines.includes(engine)){
        this.engine = engine;
      } else {
        throw new TypeError(`Templating Engine(${engine}) is not supported.`);
      }
    }

    handleDownload (specPath){
      const downloadRoute = prepareRoute(specPath);
      const hexoRoute = url_for(downloadRoute);
      return hexoRoute;
    }

    get processor (){

      const transformer = require('../lib/nodejs/swagger-to-html')({hexo});
      const engine = this.engine;
      const that = this;

      return function (args){
        const ctx = this;
        let specPath = args[0];

        if(!validUrl.isUri(specPath)){
          specPath = path.resolve(path.dirname(ctx.full_source), specPath);
        }

        let output = '';
        const transformerPromise = new Promise((resolve, reject) => {
          const readableStream = transformer(specPath);

          readableStream.on('readable', () => {
            let chunk;
            while ((chunk = readableStream.read()) !== null) {
              output += chunk;
            }
          })
          .on('end', () => {
            resolve(output);
          })
          .on('error', (err) => {
            reject(err);
          });
        });


        return transformerPromise.then((output) => {
          const downloadRoute = that.handleDownload(specPath);
          return hexo.render.render({text: output.toString(), engine: engine })
            .then((html) =>
              `<div class="doc-swagger-to-html">
                <div class="download-btn" data-download-route="${downloadRoute}">
                  <a class="download-btn__link" href="${downloadRoute}" target="_blank" download>Download Schema</a>
                </div>
                ${html}
              </div>`
            );
        });
      };
    }
  }

  hexo.extend.tag.register('swagger_to_html', new SwaggerProcessor('html').processor, {async: true});
};
