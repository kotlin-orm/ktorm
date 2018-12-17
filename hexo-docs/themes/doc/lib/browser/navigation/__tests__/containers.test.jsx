const React = require('react');
const $ = require('jquery');
const {shallow, mount} = require('enzyme');

describe('browser.navigation.containers', () => {

  describe('Navigation', () => {

    const mockSearchLoad = jest.fn()
      .mockImplementation(() => {
        return Promise.resolve(() => {
          return () => {};
        });
      });

    jest.mock('../../search/load', () => mockSearchLoad);

    const {SidebarClose, SidebarToggle} = require('../components.jsx');
    const {Navigation, NAVIGATION_IS_COLLASPED_CLASS} = require('../containers.jsx');

    const createComponent = (props) => {
      return React.createFactory(Navigation)(Object.assign({
        config: {
          root: '/',
          theme_config: {
            search: {}
          }
        },
        data: {
          navigation: {
            main: [],
            logo: {
              path: 'logo-path'
            }
          }
        },
        page: {}
      }, props));
    };



    it('should shallow render without any error', () => {
      const navigation = shallow(createComponent());
      expect(navigation.length).toEqual(1);
    });

    it('should mount without any error', () => {
      const navigation = mount(createComponent());
      expect(navigation.length).toEqual(1);
    });

    it('should not enable smoothScroll if $headers are not found in the page', () => {
      const html = '';
      document.documentElement.innerHTML = html;
      const navigation = mount(createComponent());
      expect(navigation.length).toEqual(1);
      expect(navigation.instance().smoothScroll).not.toBeDefined();
    });

    it('should enable smoothScroll if $headers are found in the page', () => {
      const html = `
        <h2>Foo</h2>
        <h2>Bar</h2>
      `;
      document.documentElement.innerHTML = html;
      const navigation = mount(createComponent());
      expect(navigation.length).toEqual(1);
      expect(navigation.instance().smoothScroll).toBeDefined();
    });

    it('should collapse navigation on SidebarClose click and uncollapse on SidebarToggle click', () => {
      const navigation = mount(createComponent());
      navigation.find(SidebarClose).last().simulate('click');
      expect($('body').attr('class')).toContain(NAVIGATION_IS_COLLASPED_CLASS);

      navigation.find(SidebarToggle).last().simulate('click');
      expect($('body').attr('class')).not.toContain(NAVIGATION_IS_COLLASPED_CLASS);
    });


    it('should listen to scroll events and update the state accordingly', () => {
      const html = `
        <div style="height: 2000px;">
          <h2 id="foo">Foo</h2>
          <h2 id="bar">Bar</h2>
        </div>
      `;
      document.documentElement.innerHTML = html;

      const navigation = mount(createComponent());

      window.dispatchEvent(new Event('scroll'));

      expect(navigation.state().visibleHeaderId).toBe('bar');
    });

    describe('navigation tree', () => {
      const navigationData = {
        main: [
          {
            type: 'link',
            text: 'Foo',
            path: '/foo/index.html',
            children: [
              {
                type: 'link',
                text: 'Bar',
                path: '/foo/bar.html',
                children: [
                  {
                    type: 'link',
                    text: 'Bar 1',
                    path: '/foo/bar-1.html',
                    children: [
                      {
                        type: 'link',
                        text: 'Bar 1.0',
                        path: '/foo/bar-1-0.html'
                      }
                    ]
                  }
                ]
              }
            ]
          }
        ]
      };

      it('should decorate navigation items with special methods and properties', () => {
        const navigation = mount(createComponent({
          data: {
            navigation: navigationData
          },
          page: {
            path: '/foo/bar.html'
          }
        }));

        const items = navigation.instance().items;

        expect(typeof items[0].hasParent).toBe('function');
        expect(typeof items[0].parent).toBe('function');
        expect(typeof items[0].isCurrent).toBe('boolean');

        expect(items[0].isCurrentAncestor).toBe(true);
        expect(items[0].children[0].isCurrent).toBe(true);
        expect(items[0].children[0].hasParent()).toBe(true);
        expect(items[0].children[0].parent()).toBe(items[0]);
      });

      it('should walk deep down the tree and set the right values for isCurrent and isCurrentAncestor for each item', () => {
        const navigation = mount(createComponent({
          data: {
            navigation: navigationData
          },
          page: {
            path: '/foo/bar-1-0.html'
          }
        }));

        const items = navigation.instance().items;

        expect(items[0].isCurrentAncestor).toBe(true);
        expect(items[0].children[0].isCurrentAncestor).toBe(true);
        expect(items[0].children[0].children[0].isCurrentAncestor).toBe(true);
        expect(items[0].children[0].children[0].children[0].isCurrent).toBe(true);
      });

      it('should display current item and all children lists are opened up to the last ancestor', () => {
        const navigation = mount(createComponent({
          data: {
            navigation: navigationData
          },
          page: {
            path: '/foo/bar-1-0.html'
          }
        }));

        const current = navigation.find('.doc-sidebar-list__item--current');
        const currentLink = current.find('.doc-sidebar-list__item--current > a').getDOMNode();
        const currentAncestorLists = current.parents('.doc-sidebar-list__children-list');

        expect(currentLink.attributes.getNamedItem('href').value).toEqual('/foo/bar-1-0.html');
        expect(currentAncestorLists.length).toEqual(3);
        currentAncestorLists.forEach((c) => {
          expect(c.hasClass('doc-sidebar-list__children-list--hidden')).toEqual(false);
        });
      });
    });
  });
});
