describe('browser.utils', () => {
  describe('url_for', () => {
    let url_for;
    beforeEach(() => {
      url_for = require('../utils').url_for({
        config: { root: '/root' }
      });
    });

    it('should create a function', () => {
      expect(typeof url_for).toBe('function');
    });

    it('should join configured root with the path passed as an argument', () => {
      expect(url_for('/index.html?query=hello#hash')).toBe('/root/index.html?query=hello#hash');
    });

    it('should remove eventual double slashes', () => {
      const url_for = require('../utils').url_for({
        config: { root: '/' }
      });
      expect(url_for('//index.html?query=hello#hash')).toBe('/index.html?query=hello#hash');
    });

    it('should return the untouched path if it\'s an absolute url ', () => {
      const path = 'https://www.google.com';
      expect(url_for(path)).toBe(path);
    });
  });
});
