'use strict';

const path = require('path');

function createHelper ({theme_config, source_dir, render, log}) {

  let partials = null;

  function projectPartialHelper (area) {

    // Get and normalize "partials" configuration once and cache the result
    //
    // We do this operation inside the helper function because:
    //
    // - it means that `project_partial` template helper is really used in the layout
    // - the rendering process is involved (it will not be excuted if you run `hexo clean`, for example)

    if (!partials) {
      partials = getPartials({
        theme_config, source_dir, render, log
      });
    }

    if (!partials[area]) { return ''; }

    // in a hexo helper plugin function,
    // "this" represents the same context that hexo pass
    // when rendering theme's layout partials
    const locals = this;
    return partials[area]
      .map((p) => renderPartial(p, locals))
      .filter(h => h)
      .join('\n');
  }

  function renderPartial (partialPath, locals) {
    try {
      return render.renderSync({ path: partialPath }, locals);
    } catch (err) {
      log.error(`There was a problem while rendering partial "${partialPath}". skip rendering.`);
      log.error(`${err.message}`);
      log.debug(err);
      return '';
    }
  }

  return projectPartialHelper;
}

function getPartials ({theme_config, source_dir, render, log}) {
  const partials = Object.assign({
    head_start: [],
    head_end: [],
    footer_start: [],
    footer_end: []
  }, theme_config.partials);

  const isValidPartial = (p) => {
    return typeof p === 'string';
  };

  const isRenderablePartial = (p) => {
    if (!render.isRenderable(p) || render.getOutput(p) !== 'html') {
      log.warn(`partial "${p}" cannot be rendered or the output is not html.`);
      return false;
    }
    return true;
  };

  // "normalize" partials for each area:
  // - always use array type for each area
  // - exclude invalid partials or non renderable partials
  return Object.keys(partials).reduce((normalizedPartials, area) => {

    if (!Array.isArray(partials[area])) {
      partials[area] = [partials[area]];
    }

    normalizedPartials[area] = partials[area]
      .filter(isValidPartial)
      .map((p) => path.resolve(source_dir, p))
      .filter(isRenderablePartial);

    return normalizedPartials;

  }, {});
}

module.exports = { getPartials, createHelper };
