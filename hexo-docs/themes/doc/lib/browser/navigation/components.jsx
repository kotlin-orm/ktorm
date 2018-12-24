const React = require('react');
const {dispatch, classNames} = require('../utils');

function Navbar (props) {
  return (
    <nav className="doc-navbar">
      {props.children}
    </nav>
  );
}

function Logo ({page, url_for}) {
  var homePath = page.lang === 'en' ? '/' : 'zh-cn/'; 

  return (
    <span className="doc-navbar__logo">
      <a href={url_for(homePath)}>
        <img src={url_for('images/logo.png')} className="doc-navbar__logo__img"/>
        <img src={url_for('images/logo-middle.png')} className="doc-navbar__logo__img-full"/>
        {/* <span className="doc-navbar__logo__text">{navigation.logo.text}</span> */}
      </a>
    </span>
  );
}

function Sidebar ({items, page, url_for, config, uncollapse, tocItems, visibleHeaderId, support}) {

  const renderItems = () => {
    const supportItems = support && support.navigation === true ? [{
      type: 'label',
      text: support.navigation_label
    }, {
      type: 'link',
      path: support.link_url,
      text: support.link_text,
      target: '_blank'
    }] : [];

    return (items || []).concat(supportItems).map((item, i) => {
      return (<SidebarItem
        key={i + 'sidebar-item' }
        item={item}
        page={page}
        config={config}
        tocItems={tocItems}
        visibleHeaderId={visibleHeaderId}
        url_for={url_for} />
      );
    });
  };

  return (
    <nav className="doc-sidebar">
      <div className="doc-sidebar__vertical-menu">
        <SidebarToggle
          className="doc-sidebar-toggle--primary doc-sidebar__vertical-menu__item"
          onClick={uncollapse} />
        <i className="dc-icon
            dc-icon--search
            dc-icon--interactive
            doc-sidebar__vertical-menu__item
            doc-sidebar__vertical-menu__item--primary"
        onClick={uncollapse}>
        </i>
      </div>

      <div className="doc-sidebar-content">
        <div className="doc-sidebar__search-form">
          <div className="dc-search-form doc-search-form">
            <input type="search"
              id="doc-search-input"
              className="dc-input dc-search-form__input doc-search-form__input"
              placeholder={page.lang === 'en' ? 'Search documents' : '搜索文档'}
              autoFocus={true} />
            <button className="dc-btn dc-search-form__btn doc-search-form__btn" aria-label="Search">
              <i className="dc-icon dc-icon--search"></i>
            </button>
          </div>
        </div>
        <ul className="doc-sidebar-list">
          { renderItems() }
        </ul>
      </div>
    </nav>
  );
}

class SidebarItem extends React.Component  {
  constructor (props) {
    super(props);

    this.state = {
      hasChildren: false,
      childrenListIsVisible: false
    };
  }

  componentDidMount () {
    const {item, page} = this.props;
    const hasChildren = Array.isArray(item.children) && item.children.length > 0;
    const childrenListIsVisible = (item.children || []).find((child) => {
      return child.path === page.path;
    }) || (hasChildren && item.isCurrent) || (hasChildren && item.isCurrentAncestor);

    this.setState({
      hasChildren,
      childrenListIsVisible
    });
  }

  toggleChildrenVisibility () {
    if (!this.state.hasChildren) { return; }
    this.setState({
      childrenListIsVisible: !this.state.childrenListIsVisible
    });
  }

  render () {
    const {item, page, url_for, tocItems, config, visibleHeaderId, className} = this.props;
    const isLabel = item.type === 'label';
    const isCurrentAncestor = item.isCurrentAncestor;
    const isCurrent = item.isCurrent;
    const hasChildren = this.state.hasChildren;
    const childrenListIsVisible = this.state.childrenListIsVisible;

    let toc = null;
    let children = null;

    if (hasChildren) {
      children = (<SidebarChildrenList
        item={item}
        page={page}
        config={config}
        tocItems={tocItems}
        visibleHeaderId={visibleHeaderId}
        url_for={url_for}
        hidden={!childrenListIsVisible}
      />);
    }

    if (isCurrent) {
      toc = (
        <ul className="doc-sidebar-list__toc-list">
          {
            (tocItems || []).map(function (i, tocItem) {
              return (<SidebarTocItem
                key={i + 'sidebar-toc-item'}
                visibleHeaderId={visibleHeaderId}
                item={tocItem}
              />);
            })
          }
        </ul>
      );
    }

    const itemClassName = classNames({
      'doc-sidebar-list__item': true,
      'doc-sidebar-list__item--label': isLabel,
      'doc-sidebar-list__item--link': !isLabel,
      'doc-sidebar-list__item--current': isCurrent,
      'doc-sidebar-list__item--current-ancestor': !!isCurrentAncestor,
      'doc-sidebar-list__item--has-children': hasChildren,
      'doc-sidebar-list__item--children-list--hidden': hasChildren && !childrenListIsVisible,
      [className]: true
    });

    const toggleClassName = classNames({
      'doc-sidebar-list__item__children-toggle': hasChildren,
      'doc-sidebar-list__item__children-toggle--show': hasChildren && !childrenListIsVisible,
      'doc-sidebar-list__item__children-toggle--hide': hasChildren && childrenListIsVisible
    });

    return (
      <li className={itemClassName}>
        {
          isLabel ? <span onClick={this.toggleChildrenVisibility.bind(this)}
            className={toggleClassName}>{item.text}</span> :
            <a
              className={toggleClassName}
              href={url_for(item.path)}
              target={item.target ? item.target : '_self'}>
              <span>{ item.text }</span>
            </a>
        }
        { toc }
        { children }
      </li>
    );
  }
}

function SidebarChildrenList ({item, page, config, tocItems, visibleHeaderId, url_for, hidden}) {

  return (<ul className={classNames({
    'doc-sidebar-list__children-list': true,
    'doc-sidebar-list__children-list--hidden': hidden
  })}>
    {
      item.children.map((child, i) => {
        return (
          <SidebarItem
            key={i + 'sidebar-child-item' }
            className="doc-sidebar-list__item--child"
            item={child}
            page={page}
            config={config}
            tocItems={tocItems}
            visibleHeaderId={visibleHeaderId}
            url_for={url_for} />
        );
      })
    }
  </ul>);
}

function SidebarTocItem ({item, visibleHeaderId}) {
  const handleOnClick = () => {
    $('#doc-search-results').hide();
    $('#page-content').show();
  };

  return (
    <li className={`doc-sidebar-list__toc-item ${item.id === visibleHeaderId ? 'doc-sidebar-list__toc-item--current' : '' }`}>
      <a href={ '#' + item.id } data-scroll onClick={handleOnClick}>
        <span>{ item.text }</span>
      </a>
    </li>);
}


function SidebarToggle ({className, onClick}) {
  return (
    <i className={'dc-icon dc-icon--menu dc-icon--interactive doc-sidebar-toggle ' + (className || '')}
      onClick={onClick}>
    </i> );
}

function SidebarClose ({className, onClick}) {
  return (
    <i className={'dc-icon dc-icon--close dc-icon--interactive doc-sidebar-close ' + (className || '')}
      onClick={onClick}>
    </i>
  );
}

module.exports = {Navbar, Logo, Sidebar, SidebarItem, SidebarToggle, SidebarClose};
