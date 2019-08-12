const React = require('react');

module.exports.SupportFooter = function ({page, data, url_for}) {
  var navigation = null;
  if (page.lang === 'zh-cn') {
    navigation = data['navigation-zh-cn'].main.filter((item) => item.type === 'link');
  } else {
    navigation = data['navigation-en'].main.filter((item) => item.type === 'link');
  }

  function renderLinks() {
    var links = [];
    var currentIndex = navigation.map((item) => item.path).indexOf(page.path.replace('index.html', ''));

    if (currentIndex !== -1 && currentIndex !== 0) {
      var previous = navigation[currentIndex - 1];
      links.push(
        <div className="doc-footer-link" key={previous.path}>
          {page.lang === 'zh-cn' ? '上一篇：' : 'Prev Article: '}
          <a href={url_for(previous.path)}>
            {previous.text}
          </a>
        </div>
      );
    }

    if (currentIndex !== -1 && currentIndex !== navigation.length - 1) {
      var next = navigation[currentIndex + 1];
      links.push(
        <div className="doc-footer-link" key={next.path}>
          {page.lang === 'zh-cn' ? '下一篇：' : 'Next Article: '}
          <a href={url_for(next.path)}>
            {next.text}
          </a>
        </div>
      );
    }

    return links;
  }

  if (page.lang === 'zh-cn') {
    return (
      <div className="doc-support-footer">
        {renderLinks()}
        对文档内容有疑问？请在侧边栏尝试搜索或者在 GitHub <a href="https://github.com/vincentlauvlwj/Ktorm/issues/new">提出 issue</a>
      </div>
    );
  } else {
    return (
      <div className="doc-support-footer">
        {renderLinks()}
        Any questions about the document? Try searching again on the left menu or <a href="https://github.com/vincentlauvlwj/Ktorm/issues/new">Raise an issue on Github</a>
      </div>
    );
  }
};
