'use strict';

const fs = require('fs');
const {merge} = require('lodash');
const {PassThrough} = require('stream');
const consolidate = require('consolidate');
consolidate.requires.ejs = require('ejs');

module.exports = {createTransformer};

/**
 * Creates a "transform" function
 *
 * @param  {TransformOptions} defaultOptions - the default options for a transformer
 * @return {function} - the transform function
 *
 * @example
 *
 * //
 * // file: content.ejs
 * //
 *
 * <div>
 *  <h1><%= pkg.name %></h1>
 *  <div><strong>version:</strong> <%= pkg.version %></div>
 *  <%- include_module('description', { description: pkg.description }) %>
 * </div>
 *
 * //
 * // file: description.ejs
 * //
 *
 * <p><%= description %></p>
 *
 * //
 * // file: footer.ejs
 * //
 *
 * <footer>
 *    <small><%= pkg.license || 'NO LICENSE' %> - 2017</small>
 * </footer>
 *
 * //
 * // file: package.json
 * //
 *
 * {
 *   "name": "foo",
 *   "version": "2.0.0",
 *   "description": "foo description"
 * }
 *
 * //
 * // file: index.js
 * //
 *
 * const transformer = createTransformer({
 *    input: (json) => Promise.resolve(require(json)),
 *    modules: {
 *      head: {
 *        order: 0,
 *        template: path.resolve(__dirname, './head.ejs'),
 *        controller: (pkg) => { return { pkg } }
 *      },
 *      description: {
 *        include: true,
 *        template: path.resolve(__dirname, './description.ejs')
 *      },
 *      footer: {
 *        order: 1,
 *        template: path.resolve(__dirname, './footer.ejs')
 *      }
 *    }
 * });
 *
 * transformer('./package.json')
 *  .pipe(process.stdout); // the output will be:
 *
 * //
 * // output
 * //
 *
 * <div>
 *  <h1>foo</h1>
 *  <div><strong>version:</strong> 2.0.0</div>
 *  <p>foo description</p>
 * </div>
 * <footer>
 *   <small>NO LICENSE - 2017</small>
 * </footer>
 *
 */

/**
 * TransformOptions
 *
 * @typedef {Object} TransformOptions
 *
 * @property {Object} modules - modules map
 * @property {Function} [input] - A function to process user input, must return a Promise<any> with the context as resolved value
 * @property {Function} [render] - custom render function, must return a Promise<String> with the compiled string as resolved value
 * @property {Function} [renderString] - custom render string function, must return the compiled string
 * */

/**
 * Create a transform function
 *
 * @public
 *
 * @param  {TransformOptions} defaultOptions [description]
 * @return {Function}
 */
function createTransformer (defaultOptions) {


  const transformer = new Transformer(defaultOptions);

  /**
   * Transform an input to a compiled text output
   *
   * @param  {any} input - user input
   * @param  {TransformOptions} [options={}] - user options overriding defaultOptions
   * @return {stream.Readable} - readable stream
   */
  return function transform (input, options = {}) {
    return transformer.transform(input, options);
  };
}

/**
 * @private
 */
class Transformer {
  constructor (options) {
    this.options = Object.assign({
      input:(ctx) => Promise.resolve(ctx),
      render: consolidate.ejs,
      renderString: consolidate.requires.ejs.render,
      modules: {}
    }, options);
  }

  /**
  /**
   * Transform user input to a compiled text output
   *
   * @param  {any} input - user input
   * @param  {TransformOptions} [options={}] - user options overriding defaultOptions
   * @return {stream.Readable} - readable stream
   */
  transform (input, options) {
    const opt = merge({}, this.options, options);
    const stream = new PassThrough();
    const modules = new Modules(opt.modules, opt);

    opt
      .input(input)
      .then((ctx) => modules.renderEach((out) => stream.push(out), ctx))
      .then(() => stream.end())
      .catch((err) => {
        stream.emit('error', err); // The emitting of an error event will end the stream. The end event will not be fired(explicitly).
      });

    return stream;
  }
}

/**
 * @private
 */
class Module {

  constructor (key, mod) {
    Object.assign(this, mod);
    this.key = key;
  }

  controller (ctx) { return ctx; }
}

/**
 * @private
 */
class Modules {

  constructor (modulesMap, options) {
    const modules = Modules.toArray(modulesMap);
    this.options = options;
    this.layoutModules = modules.filter(m => m.include !== true).sort((m1, m2) => m1.order - m2.order);
    this.includeModules = modules.filter(m => m.include === true);
    this.includeModule = this._createIncludeModuleFn({renderString : this.options.renderString});
  }

  /**
   * renderEach layout module in sequence
   *
   * Use the callback to collect or manipulate output chunks
   *
   * @param  {Function} cb - callback called each time a module is rendered
   * @param  {Object} ctx - context used by each module's controller
   * @return {Promise} - a promise that resolves when every module is rendered
   */
  renderEach (cb, ctx) {
    const render = this.options.render;
    const include_module = this.includeModule;
    return this.layoutModules.reduce((prev, mod) => {
      const locals = mod.controller(Object.assign({}, ctx));
      locals.include_module = include_module;
      return prev.then(() => {
        return render(mod.template, locals)
          .then((output) => {
            cb(output, mod);
            return output;
          });
      });
    }, Promise.resolve());
  }

  /**
   * Create `include_module` function that can be used inside templates
   *
   * @param  {Array} opt.includeModules - registered modules as includes
   * @param  {Function} [opt.renderString] - a function used to render a template syntax string
   * @return {Function} - return include_module function that can be used inside templates
   */
  _createIncludeModuleFn ({renderString}) {
    const self = this;
    return function include_module (key, mctx = {}) {
      const ctx = this;
      const mod = self.includeModules.find(m => m.key === key);

      if (!mod) { return ''; }

      const locals = mod.controller(Object.assign({}, ctx, mctx));
      locals.include_module = include_module;

      const string = fs.readFileSync(mod.template, 'utf8');

      return renderString(string, locals);
    };
  }

  /**
   * From map of simple objects creates an array of modules
   *
   * @param  {Object} modules - modules map
   * @return {Array<Module>} - array of modules
   */
  static toArray (modules) {
    return Object.keys(modules).reduce((acc, key) => {
      const mod = modules[key];
      acc.push(new Module(key, mod));
      return acc;
    }, []);
  }
}
