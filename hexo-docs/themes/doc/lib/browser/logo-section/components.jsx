const React = require('react');

class LogoSection extends React.Component {
  constructor(props) {
    super(props);

    this.page = props.page;
    this.url_for = props.url_for;
  }

  render() {
    if (this.page.path.indexOf('index.html') === -1) {
      return null;
    }

    return (
      <section className="doc-logo-section">
        <p>
          <img className="full-logo" src={this.url_for('images/logo-full.png')} alt="Ktorm" />
        </p>
        <p>
          <a href="https://www.travis-ci.org/vincentlauvlwj/Ktorm">
            <img className="badge" src="https://www.travis-ci.org/vincentlauvlwj/Ktorm.svg?branch=master" alt="Build Status" />
          </a>
          <a href="https://search.maven.org/search?q=g:%22me.liuwj.ktorm%22">
            <img className="badge" src="https://img.shields.io/maven-central/v/me.liuwj.ktorm/ktorm-core.svg?label=Maven%20Central" alt="Maven Central" />
          </a>
          <a href="https://github.com/vincentlauvlwj/Ktorm/blob/master/LICENSE">
            <img className="badge" src="https://img.shields.io/badge/license-Apache%202-blue.svg?maxAge=2592000" alt="Apache License 2" />
          </a>
          <a href="https://app.codacy.com/app/vincentlauvlwj/Ktorm?utm_source=github.com&utm_medium=referral&utm_content=vincentlauvlwj/Ktorm&utm_campaign=Badge_Grade_Dashboard">
            <img className="badge" src="https://api.codacy.com/project/badge/Grade/65d4931bfbe14fe986e1267b572bed53" alt="Codacy Badge" />
          </a>
          <a href="https://github.com/KotlinBy/awesome-kotlin">
            <img src="https://kotlin.link/awesome-kotlin.svg" alt="Awesome Kotlin Badge" />
          </a>
        </p>
      </section>
    );
  }
}

module.exports = {LogoSection};
