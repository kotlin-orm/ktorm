const React = require('react');
const ReactDOM = require('react-dom');
const $ = require('jquery');
const {url_for, getTOCHeaders} = require('../utils');
const {Sidebar, SidebarToggle, SidebarClose, Navbar, Logo} = require('./components.jsx');
const {SearchForm} = require('../search/components.jsx');
const searchLoad = require('../search/load');

const SIDEBAR_IS_VISIBLE_CLASS = 'doc-sidebar--is-visible';
const NAVIGATION_IS_COLLASPED_CLASS = 'doc-navigation--is-collapsed';

class Navigation extends React.Component {
  constructor (props) {
    super(props);

    this.url_for = url_for(this.props);

    this.state = {
      search: null,
      collapsed: false,
      tocItems: [],
      visibleHeaderId: null
    };
  }

  componentDidMount () {
    const SmoothScroll = require('smooth-scroll');
    const $headers = getTOCHeaders();
    const tocItems = this.getTocItems($headers);

    this.$body = $('body');
    this.$content = $('.doc-content');
    this.items = this.getItems();

    // this selector is wrapped in a function
    // since the selected element can be removed and recreated depending on the state
    // we have to access the DOM everytime, we can't keep a reference
    this.$searchFormInput = () => $('.dc-search-form__input');

    this.loadSearchIndex();
    this.addAnchorToHeaders($headers);
    this.listenContentClick();
    this.listenVisibleHeaderChanges($headers);

    if ($headers.length) {
      this.smoothScroll = new SmoothScroll('a[data-scroll]', {
        speed: 400
      });
    }

    this.setState({
      tocItems,
      visibleHeaderId: window.location.hash.replace('#', '')
    });
  }

  getItems () {
    const {page} = this.props;
    const {navigation} = Object.assign({}, { navigation: {} }, this.props.data);
    const items = navigation.main || [];

    (function recurse (items, parent) {
      items.forEach((item) => {
        // add parent methods
        item.parent = () => { return parent; };
        item.hasParent = () => {
          return !!item.parent();
        };

        // check if the item represents the current page,
        // and traverse ancestors
        if (item.path === page.path) {
          item.isCurrent = true;
          (function walk (p) {
            if (p) {
              p.isCurrentAncestor = true;
            }
            if (p && p.hasParent()) {
              walk(p.parent());
            }
          })(item.parent());
        } else {
          item.isCurrent = false;
        }

        if (item.children && item.children.length > 0) {
          recurse(item.children, item);
        }
      });
    })(items);

    return items;
  }

  getTocItems ($headers) {
    return $headers.map(function (i, h) {
      return {
        id: h.id,
        text: (h.title || h.textContent),
        tagName: h.tagName
      };
    });
  }

  addAnchorToHeaders ($headers) {
    $headers.each(function makeHeaderLinkable (i, h) {
      const span = document.createElement('span');
      h.insertBefore(span, h.firstChild);

      ReactDOM.render((
        <a
          className="doc-anchor"
          href={'#' + h.id}
          aria-hidden={true}
          data-scroll>
        </a>
      ), span);
    });
  }


  // Listen to "DOMContentLoaded|scroll|resize" events and determines
  // which header is currently "visible"
  listenVisibleHeaderChanges ($headers) {
    const offsetThreshold =  120;
    let prev, next;

    const listener = () => {
      const doc = document.documentElement;
      const top = doc && doc.scrollTop || document.body.scrollTop;
      const end = $(window).scrollTop() + $(window).height() === $(document).height();
      let last;

      if (!end) {
        for (let i = 0; i < $headers.length; i++) {
          const link = $headers[i];
          if (link.offsetTop - offsetThreshold > top) {
            if (!last) last = link;
            break;
          } else {
            last = link;
          }
        }
        next = last;
      }

      if (end && $headers.length) {
        next = $headers[$headers.length - 1];
      }

      if (next && prev !== next) {
        const $next = $(next);
        const id = $next.attr('id') || $next.children('div').attr('id');
        this.setState({
          visibleHeaderId: id
        });
        prev = next;
      }
    };

    document.addEventListener('DOMContentLoaded', listener, false);
    window.addEventListener('scroll', listener);
    window.addEventListener('resize', listener);

    return listener;
  }

  loadSearchIndex () {
    const route = this.props.config.theme_config.search.route || '/lunr.json';
    searchLoad(this.url_for(route))
      .then((search) => this.setState({ search }));
  }

  listenContentClick () {
    this.$content.on('click', this.onContentClick.bind(this));
  }

  onContentClick () {
    if ( this.$body.hasClass(SIDEBAR_IS_VISIBLE_CLASS) ) {
      this.toggleSidebar();
    }
  }

  collapseSidebar () {
    this.$body.addClass(NAVIGATION_IS_COLLASPED_CLASS);
  }

  uncollapseSidebar () {
    this.$body.removeClass(NAVIGATION_IS_COLLASPED_CLASS);
    this.$searchFormInput().focus();
  }

  toggleSidebar () {
    this.$body.toggleClass(SIDEBAR_IS_VISIBLE_CLASS);
  }

  hideSidebar () {
    this.$body.removeClass(SIDEBAR_IS_VISIBLE_CLASS);
  }

  render () {
    const {navigation} = Object.assign({}, { navigation: {} }, this.props.data);
    return (
      <div className="doc-navigation">
        <Navbar
          config={this.props.config}
          data={this.props.data}
          url_for={this.url_for}>
          <Logo url_for={this.url_for} navigation={navigation} />
          <SidebarClose
            className="doc-navbar__sidebar-close doc-navbar__sidebar-close--desktop"
            onClick={this.collapseSidebar.bind(this)} />
          <SidebarToggle
            className="doc-navbar__sidebar-toggle"
            onClick={this.toggleSidebar.bind(this)} />
          <SearchForm
            search={this.state.search}
            onSearch={this.hideSidebar.bind(this)} />
        </Navbar>

        <Sidebar
          url_for={this.url_for}
          items={this.items}
          page={this.props.page}
          config={this.props.config}
          search={this.state.search}
          hide={this.hideSidebar.bind(this)}
          uncollapse={this.uncollapseSidebar.bind(this)}
          tocItems={this.state.tocItems}
          visibleHeaderId={this.state.visibleHeaderId}
          support={this.props.config.theme_config.support}
        />
      </div>
    );
  }
}

module.exports = {Navigation, SIDEBAR_IS_VISIBLE_CLASS, NAVIGATION_IS_COLLASPED_CLASS};
