const React = require('react');
const SupportFooterComponent = require('./components.jsx').SupportFooter;

class SupportFooter extends React.Component {
  constructor (props) {
    super(props);
  }

  render () {
    if (!this.props.page.support) { return null; }

    return (<SupportFooterComponent support={this.props.page.support} />);
  }
}

module.exports = {SupportFooter};
