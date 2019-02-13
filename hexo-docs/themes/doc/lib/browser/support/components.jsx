const React = require('react');

module.exports.SupportFooter = function ({page, data, url_for}) {
  var navigation = data["navigation-" + page.lang];

  var current = null, previous = null, next = null;

  for (var i = 0; i < navigation.main.length; i++) {
    var item = navigation.main[i];

    if (item.type === 'link' && !/^(f|ht)tps?:\/\//i.test(item.path)) {
      if (current != null && current.path === page.path.replace('index.html', '')) {
        next = item;
        break;
      }

      previous = current;
      current = item;
    }
  }

  function renderLinks() {
    var links = [];

    if (previous != null) {
      links.push(
        <div className="doc-footer-link" key={previous.path}>
          {page.lang === 'en' ? 'Prev Article: ' : '上一篇：'}
          <a href={url_for(previous.path)}>
            {previous.text}
          </a>
        </div>
      );
    }

    if (next != null) {
      links.push(
        <div className="doc-footer-link" key={next.path}>
          {page.lang === 'en' ? 'Next Article: ' : '下一篇：'}
          <a href={url_for(next.path)}>
            {next.text}
          </a>
        </div>
      );
    }

    return links;
  }

  if (page.lang === 'en') {
    return (
      <div className="doc-support-footer">
        {renderLinks()}
        Any questions about the document? Try searching again on the left menu or <a href="https://github.com/vincentlauvlwj/Ktorm/issues/new">Raise an issue on Github</a>
      </div>
    );
  } else {
    return (
      <div className="doc-support-footer">
        {renderLinks()}
        对文档内容有疑问？请在侧边栏尝试搜索或者在 GitHub <a href="https://github.com/vincentlauvlwj/Ktorm/issues/new">提出 issue</a>
      </div>
    );
  }
};
