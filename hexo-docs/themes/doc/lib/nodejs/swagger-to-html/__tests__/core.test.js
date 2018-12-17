'use strict';

const path = require('path');
const {createTransformer} = require('../core');

describe('swagger-processor.core', () => {

  describe('createTransformer', () => {
    const testTranformer = createTransformer({
      modules: {
        body: {
          order: 1,
          template: path.resolve(__dirname, './templates/body.ejs'),
          controller: (ctx) => {
            return { text: ctx.body_text };
          }
        },
        head: {
          order: 0,
          template: path.resolve(__dirname, './templates/head.ejs')
        }
      }
    });

    it('should be a function', () => {
      expect(typeof testTranformer).toEqual('function');
    });

    it('should transform the input in the expected output', (done) => {
      const data = [];
      testTranformer({
        title: 'title',
        body_text: 'body'
      }).on('data', (chunk) => {
        data.push(chunk.toString());
      }).on('error', done.fail)
        .on('end', () => {
          expect(data[0].trim()).toEqual('title');
          expect(data[1].trim()).toEqual('body');
          done();
        });
    });

    it('should transform the input in the expected output in the right order', (done) => {
      const delayedTransformer = createTransformer({
        render: (tpl) => {
          // "inject" a custom render function to emulate a delay
          const delay = tpl === 'a' ? 500 : 0;
          return new Promise((resolve) => {
            setTimeout(() => {
              resolve(tpl);
            }, delay);
          });
        },
        modules: {
          b: {
            order: 1,
            template: 'b'
          },
          a: {
            order: 0,
            template: 'a'
          }
        }
      });

      const data = [];
      delayedTransformer().on('data', (chunk) => {
        data.push(chunk.toString());
      }).on('error', done.fail)
        .on('end', () => {
          expect(data[0].trim()).toEqual('a');
          expect(data[1].trim()).toEqual('b');
          done();
        });
    });

    it('should emit error and close the stream when input function rejects', (done) => {
      const testError = new Error('test error');
      const failingTransformer = createTransformer({
        input: () =>  Promise.reject(testError),
        modules: {}
      });
      failingTransformer().on('error', (err) => {
        expect(err).toBe(testError);
        done();
      });
    });

    it('should emit error and close the stream when controller function throw an error', (done) => {
      const testError = new Error('test error');
      const failingTransformer = createTransformer({
        modules: {
          foo: {
            controller: () => { throw testError; }
          }
        }
      });
      failingTransformer().on('error', (err) => {
        expect(err).toBe(testError);
        done();
      });
    });
  });

  describe('include_module', () => {
    const includeModuleTestTransformer = createTransformer({
      modules: {
        head: {
          template: path.resolve(__dirname, './templates/head-include-module.ejs')
        },
        subtitle: {
          include: true,
          template: path.resolve(__dirname, './templates/includes/subtitle.ejs')
        },
        description: {
          include: true,
          template: path.resolve(__dirname, './templates/includes/description.ejs')
        }
      }
    });

    it('should transform the input using include_module template helper', (done) => {
      const data = [];
      includeModuleTestTransformer({
        title: 'title',
        subtitle: 'subtitle'
      }).on('data', (chunk) => {
        data.push(chunk.toString());
      }).on('error', done.fail).on('end', () => {
        expect(data[0].trim()).toEqual('title\n\nsubtitle\n\ndescription');
        done();
      });
    });

    it('should return an empty string for a non-existent module', (done) => {
      const data = [];
      const transformer = createTransformer({
        modules: {
          head: {
            template: path.resolve(__dirname, './templates/head-include-module-doesnt-exist.ejs')
          }
        }
      });

      transformer({
        title: 'title',
      }).on('data', (chunk) => {
        data.push(chunk.toString());
      }).on('error', done.fail).on('end', () => {
        expect(data[0].trim()).toEqual('title');
        done();
      });
    });

  });
});
