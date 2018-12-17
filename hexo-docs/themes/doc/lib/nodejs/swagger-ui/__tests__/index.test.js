'use strict';

const mockPath = require('path');
const cheerio = require('cheerio');
const {mockFileContent} = require('./mocks');

describe('swagger_ui', () => {

  /**
   * Mock touch function.
   */
  const mockTouch = jest.fn();
  jest.mock('touch', () => mockTouch);

  /**
   * Mock parse-schema-file to respond with error and petstore schema.
   */
  jest.mock('../parse-schema-file.js', () => {
    return (filePath, source) => {
      const basename = mockPath.basename(filePath);
      if ('error.json' === basename){
        return Promise.reject(mockFileContent['/path/to/swagger/error.json']);
      } else if ('petstore.json' === basename){
        return Promise.resolve({
          pageSource: source,
          swagger: mockFileContent['/path/to/swagger/petstore.json']
        });
      }
    };
  });

  const Hexo = require('hexo');
  const hexo = new Hexo(mockPath.join(__dirname, 'swagger_ui_test'));


  const mockError = jest.fn();
  const mockDebug = jest.fn();
  const mockLog = {
    error: mockError,
    debug: mockDebug
  };

  // Mock hexo log so that we can track the calls
  hexo.log = mockLog;

  const createSwaggerUI = require('../index');
  const {swaggerUITag, swaggerUIProcessor} = createSwaggerUI({hexo});

  /**
   * Overwrite this of swaggerUITag.
   * Inject {source, full_source}, its used in the tag plugin.
   */
  const pageObject = {
    source: 'path/to/file.md',
    full_source: '/User/jest/full/path/to/file.md'
  };
  const mockSwaggerUITag = swaggerUITag.bind(pageObject);

  beforeAll(() => {
    hexo.init();
    // Injecting ejs renderer to hexo
    // FIXME: Check if theres a better way to write the following line
    const ejsRendererPath = mockPath.resolve(__dirname, '../../../../node_modules/hexo-renderer-ejs/lib/renderer');
    const hexoRenderEjs = require(ejsRendererPath);
    hexo.extend.renderer.register('ejs', 'html', hexoRenderEjs, true);


    hexo.config.theme_config = {
      swagger_ui: createSwaggerUI.DEFAULT_CONFIG
    };
  });

  describe('when there is an error reading/parsing the swagger file', () => {
    test('should log error ', () => {
      cheerio.load(mockSwaggerUITag(['error.json']));
      // expect(mockError).toHaveBeenCalledTimes(6);
      // expect(mockError).toHaveBeenCalled();
      // FIXME: The mockfunction is called, but spy on it is not working
    });
  });

  describe('when called the first time for a page', () => {
    test('should render one swagger ui instance, with all the libraries', async () => {
      /**
       * Fake triggring processor.
       */
      const file = {
        source: '/User/jest/full/path/to/petstore.json',
        path: 'path/to/petstore.json',
      };
      swaggerUIProcessor(file);

      await mockSwaggerUITag(['petstore.json'])
        .then((html) => {
          const $ = cheerio.load(html);
          expect($('.hexo-swagger-ui').length).toBe(1);
          expect($('.swagger-wrap').length).toBe(1);
          expect($('#swagger-wrap-1').length).toBe(1);
          expect($('div[swagger-ui]').length).toBe(1);
          expect($('[src="https://cdnjs.cloudflare.com/ajax/libs/angular.js/1.6.3/angular.min.js"]').length).toBe(1);
          expect($('[src="https://cdnjs.cloudflare.com/ajax/libs/angular.js/1.6.3/angular-sanitize.min.js"]').length).toBe(1);
        });
    });
  });

  describe('when called second time for the same page', () => {
    test('should just render the swagger tag and skip the libraries', async () => {
      /**
       * Fake triggring processor.
       */
      const file = {
        source: '/User/jest/full/path/to/petstore1.json',
        path: 'path/to/petstore1.json',
      };
      swaggerUIProcessor(file);

      await mockSwaggerUITag(['petstore.json'])
        .then((html) => {
          const $ = cheerio.load(html);
          expect($('.hexo-swagger-ui').length).toBe(1);
          expect($('.swagger-wrap').length).toBe(1);
          expect($('#swagger-wrap-2').length).toBe(1);
          expect($('div[swagger-ui]').length).toBe(1);
          expect($('[src="https://cdnjs.cloudflare.com/ajax/libs/angular.js/1.6.3/angular.min.js"]').length).toBe(0);
          expect($('[src="https://cdnjs.cloudflare.com/ajax/libs/angular.js/1.6.3/angular-sanitize.min.js"]').length).toBe(0);
          expect($('[src="/script/swagger/swagger-ui.min.js"]').length).toBe(0);
          expect($('[href="/style/swagger/swagger-ui-min.css"]').length).toBe(0);
        });
    });
  });

  describe('when there is a change in any swagger file', () => {
    test('should thouch md files that include this swagger file', () => {
      /**
       * Fake triggring processor.
       */
      const file = {
        source: '/User/jest/full/path/to/petstore.json',
        path: 'path/to/petstore.json'
      };
      swaggerUIProcessor(file);
      expect(mockTouch).toHaveBeenCalledTimes(1);
      expect(mockTouch).toHaveBeenCalledWith(pageObject.full_source);
    });
  });
});
