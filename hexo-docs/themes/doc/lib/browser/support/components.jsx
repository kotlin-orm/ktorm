const React = require('react');

function SupportFooter ({support}) {
  return (
    <div className="doc-support-footer">
      <span className="doc-support-footer__text" dangerouslySetInnerHTML={{ __html: support.text }}></span>
      &nbsp;<a href={support.link_url} target="_blank" className="doc-support-footer__link">{support.link_text}</a>
    </div>
  );
}


module.exports = {SupportFooter};
