const React = require('react');

module.exports.LangSwitcher = function({page, url_for}) {
  if (page.lang === 'en') {
    return (
      <div className="lang-switcher">
        <span>
          <img src={url_for('images/us.png')}/>
          <span>English</span>
        </span>
        <span className="lang-divider"> | </span>
        <span>
          <img src={url_for('images/cn.png')}/>
          <a href={url_for(page.related_path)}>简体中文</a>
        </span>
      </div>
    );
  } else {
    return (
      <div className="lang-switcher">
        <span>
          <img src={url_for('images/cn.png')}/>
          <span>简体中文</span>
        </span>
        <span className="lang-divider"> | </span>
        <span>
          <img src={url_for('images/us.png')}/>
          <a href={url_for(page.related_path)}>English</a>
        </span>
      </div>
    );
  }
}
