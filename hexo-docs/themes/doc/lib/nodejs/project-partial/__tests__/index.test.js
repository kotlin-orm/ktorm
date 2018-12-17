'use strict';

const {getPartials, createHelper} = require('../index');
const {merge} = require('lodash');

const mockLogger = {
  debug: jest.fn(),
  info () {},
  warn () {},
  error: jest.fn()
};

describe('project-partial.createHelper', () => {
  const defaultScenario = {
    source_dir: '/root',
    log: mockLogger,
    render: {
      isRenderable: () => true,
      getOutput: () => 'html',
      renderSync: jest.fn().mockReturnValue('foo')
    },
    theme_config: {
      partials: {
        head_start: './foo.ejs'
      }
    },
    area: 'head_start'
  };

  const scenarios = [{
    message: 'should render one partial for head_start area',
    expected: {
      helper_output: 'foo'
    }
  },
  {
    message: 'should render multiple partials for head_start area',
    theme_config: {
      partials: {
        head_start: ['./foo.ejs', './foo.ejs', './foo.ejs']
      }
    },
    expected: {
      helper_output: 'foo\nfoo\nfoo'
    }
  }, {
    message: 'should render empty string because error occured while rendering',
    render: {
      renderSync: () => { throw new Error('foo'); }
    },
    expected: {
      error: true,
      helper_output: ''
    }
  },{
    message: 'should render empty string because area doesn\'t exist',
    area: 'foo_bar',
    expected: {
      helper_output: ''
    }
  }];

  scenarios.map((scenario) => {
    const s = merge({}, defaultScenario, scenario);
    s.log.error.mockClear();
    const helper = createHelper(s);

    it(s.message, () => {
      expect(s.log.error).not.toHaveBeenCalled();
      expect(typeof helper).toEqual('function');
      expect(helper(s.area)).toEqual(s.expected.helper_output);
      expect(helper(s.area)).toEqual(s.expected.helper_output);

      if (s.expected.error) {
        expect(s.log.error).toHaveBeenCalled();
      }
    });
  });
});

describe('project-partial.getPartials', () => {
  const defaultScenario = {
    source_dir: '/root',
    log: mockLogger,
  };

  const scenarios = [{
    render: {
      isRenderable: () => true,
      getOutput: () => 'html'
    },
    theme_config: {
      partials: {
        head_start: './foo.ejs'
      }
    },
    expected: {
      head_start: ['/root/foo.ejs']
    }
  },
  {
    render: {
      isRenderable: () => true,
      getOutput: () => 'jpeg'
    },
    theme_config: {
      partials: {
        head_start: './foo.ejs'
      }
    },
    expected: {
      head_start: []
    }
  }];

  scenarios.map((scenario) => {
    const s = Object.assign({}, defaultScenario, scenario);
    const partials = getPartials(s);

    it('should return array for every area', () => {
      Object.keys(partials).forEach((area) => {
        const p = partials[area];
        expect(Array.isArray(p)).toBe(true);
      });
    });

    it('should transform a value into an array with one element', () => {
      expect(partials.head_start.length).toEqual(s.expected.head_start.length);
    });

    it('should resolve partials\' path relative to project source dir', () => {
      expect(partials.head_start).toEqual(s.expected.head_start);
    });
  });
});
