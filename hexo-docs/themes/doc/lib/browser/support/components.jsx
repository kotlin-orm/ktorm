const React = require('react');

module.exports.SupportFooter = function ({page}) {
  if (page.lang === 'en') {
    return (
      <div className="doc-support-footer">
        Any questions about the document? <br/>
        Try searching again on the left menu or <a href="https://github.com/vincentlauvlwj/Ktorm/issues/new">Raise an issue on Github.</a>
      </div>
    );
  } else {
    return (
      <div className="doc-support-footer">
        对文档内容有疑问？ <br/>
        请在侧边栏尝试搜索或者在 GitHub <a href="https://github.com/vincentlauvlwj/Ktorm/issues/new">提出 issue</a>
      </div>
    );
  }
};
