const React = require('react');
const {shallow} = require('enzyme');

describe('browser.support.conatiners', () => {
  describe('SupportFooter', () => {
    const {SupportFooter} = require('../containers.jsx');

    it('should not render when page.support is falsy', () => {
      const page = {
        support: false
      };
      const supportFooter = shallow(<SupportFooter page={page} />);
      expect(supportFooter.getElement()).toBe(null);
    });

    it('should render when page.support is not falsy', () => {
      const page = {
        support: true
      };
      const supportFooter = shallow(<SupportFooter page={page} />);
      expect(supportFooter.getElement()).not.toBe(null);
    });
  });
});
