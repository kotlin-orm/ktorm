const urljoin = require('url-join');
const $ = require('jquery');

/**
 * Creates a function that mimic
 * url_for hexo helper function
 *
 * @param  {Object} props - configuration properties
 * @return {Function} url_for function
 */
function url_for (props) {
  return function (path) {
    if (/^(f|ht)tps?:\/\//i.test(path)) {
      return path;
    }
    const url = urljoin(
      props.config.root,
      path
    );
    return url.replace(/\/{2,}/g, '/'); // removes double slashes
  };
}

/**
 * Get heading html nodes in the current page
 * @return {jQuery} - a jQuery object
 */
function getTOCHeaders () {
  return $('h2');
}

/**
 * dispatch an event
 * @param  {string} eventType
 * @param  {Object} [payload={}] - payload of the event
 * @return void
 */
function dispatch (eventType, payload = {}) {
  const evt = new CustomEvent(eventType, { detail: payload });
  window.dispatchEvent(evt);
}

/**
 * subscribe to a specific event
 *
 * @param  {string} eventType
 * @param  {Function} cb - function called when the event is dispatched
 * @return {Function} - unsusscribe function
 */
function subscribeOn (eventType, cb) {
  const handler = (e) => {
    cb(Object.assign({}, {type: e.type}, e.detail));
  };

  window.addEventListener(eventType, handler);

  return () => {
    window.removeEventListener(eventType, handler);
  };
}

/**
 * Reduce a map of [string]:boolean into a string,
 * concatenating the string keys when boolean value is true
 *
 * @exmaple
 *
 * // Button Component
 *
 * function Button(props) {
 *   const className = classNames({
 *     'btn': true,
 *     'btn-disabled': props.disabled,
 *     [props.classNames]: true // append classNames passed as props if any
 *   });
 *
 *   return (
 *      <button className={className}>{props.text}</button>
 *   )
 * }
 *
 * // Usage
 *
 * <Button
 *    disabled={true}
 *    className="btn-custom"
 *    text="my button"/>
 *
 * // HTML
 *
 * <button class="btn btn-disabled btn-custom">my button</button>
 *
 * @param  {Object} [map={}]
 * @return {string}
 */
function classNames (map = {}) {
  return Object.keys(map).reduce((acc, key) => {
    if (typeof key !== 'string' || key === 'undefined') { return acc; }
    if (map[key]) {
      return acc.concat(key);
    }
    return acc;
  }, []).join(' ');
}


module.exports = {url_for, getTOCHeaders, dispatch, subscribeOn, classNames};
