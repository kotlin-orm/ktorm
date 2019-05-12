const React = require('react');

class LangSwitcher extends React.Component {
  constructor(props) {
    super(props);

    this.page = props.page;
    this.url_for = props.url_for;
  }

  render() {
    if (this.page.lang === 'zh-cn') {
      return (
        <span className="lang-switcher">
          <span>
            <img src={this.url_for('images/cn.png')}/>
            <span>简体中文</span>
          </span>
          <span className="lang-divider"> | </span>
          <span>
            <img src={this.url_for('images/us.png')}/>
            {this.renderLink('English')}
          </span>
        </span>
      );

    } else {
      return (
        <span className="lang-switcher">
          <span>
            <img src={this.url_for('images/us.png')}/>
            <span>English</span>
          </span>
          <span className="lang-divider"> | </span>
          <span>
            <img src={this.url_for('images/cn.png')}/>
            {this.renderLink('简体中文')}
          </span>
        </span>
      );
    }
  }

  renderLink(text) {
    if (this.page.related_path) {
      return <a href={this.url_for(this.page.related_path)}>{text}</a>
    } else {
      return <span style={{color: 'lightgray'}}>{text}</span>
    }
  }
}

module.exports = {LangSwitcher};
