<a name="1.0.0-rc.1"></a>
# [1.0.0-rc.1](https://github.com/zalando-incubator/hexo-theme-doc/compare/1.0.0-rc...1.0.0-rc.1) (2018-05-30)


### Bug Fixes

* **swagger-ui:** Update version of swagger UI ([a92652d](https://github.com/zalando-incubator/hexo-theme-doc/commit/a92652d))



<a name="1.0.0-rc"></a>
# [1.0.0-rc](https://github.com/zalando-incubator/hexo-theme-doc/compare/0.1.2...1.0.0-rc) (2018-03-22)


### Bug Fixes

* **navigation:** display opened ancestor list ([652f7d6](https://github.com/zalando-incubator/hexo-theme-doc/commit/652f7d6)), closes [#116](https://github.com/zalando-incubator/hexo-theme-doc/issues/116)
* **travis:** Indentation of jsx and travis config. ([05ac2c2](https://github.com/zalando-incubator/hexo-theme-doc/commit/05ac2c2))


### Code Refactoring

* **sass:** use include-path to avoid relative imports ([b635318](https://github.com/zalando-incubator/hexo-theme-doc/commit/b635318)), closes [#101](https://github.com/zalando-incubator/hexo-theme-doc/issues/101) [#101](https://github.com/zalando-incubator/hexo-theme-doc/issues/101)


### BREAKING CHANGES

* **sass:** Users utilizing hexo-theme-doc and sass customization are asked to update their configuration file(s) like the standard `_config.yml` and add:

```yaml
node_sass:
  includePaths:
		- node_modules
```



<a name="0.1.2"></a>
## [0.1.2](https://github.com/zalando-incubator/hexo-theme-doc/compare/0.1.1...0.1.2) (2018-01-02)


### Bug Fixes

* **swagger:** swagger-ui v3 download link ([5491804](https://github.com/zalando-incubator/hexo-theme-doc/commit/5491804)), closes [#95](https://github.com/zalando-incubator/hexo-theme-doc/issues/95)



<a name="0.1.1"></a>
## [0.1.1](https://github.com/zalando-incubator/hexo-theme-doc/compare/0.1.0...0.1.1) (2017-12-20)


### Bug Fixes

* **swagger:** Parser(unmerge) generating invalid schema. ([fcc7d15](https://github.com/zalando-incubator/hexo-theme-doc/commit/fcc7d15))



<a name="0.1.0"></a>
# [0.1.0](https://github.com/zalando-incubator/hexo-theme-doc/compare/0.1.0-beta.4...0.1.0) (2017-12-12)


### Bug Fixes

* **logo-text:** long logo text breaks layout ([1dd4d25](https://github.com/zalando-incubator/hexo-theme-doc/commit/1dd4d25)), closes [#26](https://github.com/zalando-incubator/hexo-theme-doc/issues/26)
* **swagger:** Preserve Model/Classes in swagger while applying decorators. ([44b8e35](https://github.com/zalando-incubator/hexo-theme-doc/commit/44b8e35))
* **swagger_to_html:** sample response including mime type ([48c902c](https://github.com/zalando-incubator/hexo-theme-doc/commit/48c902c))


### Features

* **content:** use icon link for anchors ([5d6f46c](https://github.com/zalando-incubator/hexo-theme-doc/commit/5d6f46c))
* **project-partial:** add new feature ([a98c6dd](https://github.com/zalando-incubator/hexo-theme-doc/commit/a98c6dd)), closes [#25](https://github.com/zalando-incubator/hexo-theme-doc/issues/25)
* **swagger-parser:** configurable parser options ([7c00312](https://github.com/zalando-incubator/hexo-theme-doc/commit/7c00312)), closes [#38](https://github.com/zalando-incubator/hexo-theme-doc/issues/38)
* **swagger-ui:** add swagger-ui v3.6.0 (first iteration) ([da39676](https://github.com/zalando-incubator/hexo-theme-doc/commit/da39676)), closes [#24](https://github.com/zalando-incubator/hexo-theme-doc/issues/24)



<a name="0.1.0-beta.4"></a>
# [0.1.0-beta.4](https://github.com/zalando-incubator/hexo-theme-doc/compare/0.1.0-beta.3...0.1.0-beta.4) (2017-11-13)


### Bug Fixes

* **sidebar:** fix issue with position of TOC ([2997b9b](https://github.com/zalando-incubator/hexo-theme-doc/commit/2997b9b))



<a name="0.1.0-beta.3"></a>
# [0.1.0-beta.3](https://github.com/zalando-incubator/hexo-theme-doc/compare/0.1.0-beta.2...0.1.0-beta.3) (2017-11-10)


### Bug Fixes

* **swagger_to_html**: Add -X to curl examples ([7ae1d8e](https://github.com/zalando-incubator/hexo-theme-doc/commit/7ae1d8e))
* **search:** generate index also when hexo deploy ([7027edb](https://github.com/zalando-incubator/hexo-theme-doc/commit/7027edb)), closes [#35](https://github.com/zalando-incubator/hexo-theme-doc/issues/35)
* **swagger**: Fix issue with yaml parsing of swaggerObject ([9098e12](https://github.com/zalando-incubator/hexo-theme-doc/commit/9098e12))


### Features

* **swagger**: Process x-doc.exclude to remove entities from api specification ([56a0c12](https://github.com/zalando-incubator/hexo-theme-doc/commit/56a0c12))
* **swagger**: Process x-doc.host to use a different host for documentation ([20bd1b3](https://github.com/zalando-incubator/hexo-theme-doc/commit/20bd1b3))
* **navigation:** improve children visualization ([93e1a9d](https://github.com/zalando-incubator/hexo-theme-doc/commit/93e1a9d))



<a name="0.1.0-beta.2"></a>
# [0.1.0-beta.2](https://github.com/zalando-incubator/hexo-theme-doc/compare/0.1.0-beta.1...0.1.0-beta.2) (2017-10-27)


### Features

* **swagger-ui:** add config, permalinks and ability to turn off api explorer ([5bd6a82](https://github.com/zalando-incubator/hexo-theme-doc/commit/5bd6a82))


<a name="0.1.0-beta.1"></a>
# [0.1.0-beta.1](https://github.com/zalando-incubator/hexo-theme-doc/compare/0.1.0-beta...0.1.0-beta.1) (2017-10-20)


### Bug Fixes

* **fetch:** polyfill fetch ([de320d5](https://github.com/zalando-incubator/hexo-theme-doc/commit/de320d5)), closes [#18](https://github.com/zalando-incubator/hexo-theme-doc/issues/18)



<a name="0.1.0-beta"></a>
# [0.1.0-beta](https://github.com/zalando-incubator/hexo-theme-doc/compare/0.1.0-alpha...0.1.0-beta) (2017-10-19)

### Bug Fixes

* **doc-formatting:** restore table and code styles ([9f41dbc](https://github.com/zalando-incubator/hexo-theme-doc/commit/9f41dbc))
* **layout:** import ubuntu font ([fd756eb](https://github.com/zalando-incubator/hexo-theme-doc/commit/fd756eb))
* **navigation:** smooth-scroll server side rendered ([6ee588a](https://github.com/zalando-incubator/hexo-theme-doc/commit/6ee588a))
* **nodejs-react:** fix navigation rendering ([cc4b797](https://github.com/zalando-incubator/hexo-theme-doc/commit/cc4b797))
* **react:** reintroduce node-jsx ([6c9c03a](https://github.com/zalando-incubator/hexo-theme-doc/commit/6c9c03a))
* **search:** search input on ubuntu webkit ([2f03d28](https://github.com/zalando-incubator/hexo-theme-doc/commit/2f03d28))
* **search-results:** fix SupportFooter breaking search results ([c9a399d](https://github.com/zalando-incubator/hexo-theme-doc/commit/c9a399d))
* **swagger-to-html:** Fix issue with download. ([d60930a](https://github.com/zalando-incubator/hexo-theme-doc/commit/d60930a))
* **swagger-to-html:** Fix styling of code sample ([b19dfa6](https://github.com/zalando-incubator/hexo-theme-doc/commit/b19dfa6))
* **swagger-to-html:** Update README ([fb77041](https://github.com/zalando-incubator/hexo-theme-doc/commit/fb77041))


### Features

* **optimization**: distribute minified asset
* **react**: update to react 16
* **misc:** add search-form, serch-btn ([81bdf0e](https://github.com/zalando-incubator/hexo-theme-doc/commit/81bdf0e))
* **new-design:** add navigation redesign ([adcc18e](https://github.com/zalando-incubator/hexo-theme-doc/commit/adcc18e))
* **sidebar:** collapsable children ([d65a6b8](https://github.com/zalando-incubator/hexo-theme-doc/commit/d65a6b8))
* **support:** add support functionality ([925c2f5](https://github.com/zalando-incubator/hexo-theme-doc/commit/925c2f5))
* **swagger-to-html:** Add feedback to copy button ([d4c405e](https://github.com/zalando-incubator/hexo-theme-doc/commit/d4c405e))
* **swagger-to-html:** Add security definitions ([fb02f2d](https://github.com/zalando-incubator/hexo-theme-doc/commit/fb02f2d))
* **swagger-to-html:** Provide way to download schema ([a969985](https://github.com/zalando-incubator/hexo-theme-doc/commit/a969985))
* **swagger-to-html:** Update styles according to dress-code ([67cac63](https://github.com/zalando-incubator/hexo-theme-doc/commit/67cac63))
* **typography:** style `<hr>` tags ([0b8ec2c](https://github.com/zalando-incubator/hexo-theme-doc/commit/0b8ec2c))


<a name="0.1.0-alpha"></a>
# 0.1.0-alpha (2017-09-12)

First **alpha** version

### Bug Fixes

* **search:** fix root path when build in background ([b710648](https://github.com/zalando-incubator/hexo-theme-doc/commit/b710648))
* fix issue with scrollIntoView ([b8628db](https://github.com/zalando-incubator/hexo-theme-doc/commit/b8628db))
* **browser.sidebar:** add some hot fixes to the ugly javascript inherited from meteor ([b6f6da2](https://github.com/zalando-incubator/hexo-theme-doc/commit/b6f6da2))
* **ga:** google analytics never active on localhost ([41830a4](https://github.com/zalando-incubator/hexo-theme-doc/commit/41830a4))
* **swagger-to-html:** add support for array and allOf ([3ad2ec1](https://github.com/zalando-incubator/hexo-theme-doc/commit/3ad2ec1))
* **swagger-to-html:** fix additional properties ([7c0b886](https://github.com/zalando-incubator/hexo-theme-doc/commit/7c0b886))
* **swagger-to-html:** Fix issue with styling of tables ([9f0606f](https://github.com/zalando-incubator/hexo-theme-doc/commit/9f0606f))
* **swagger-to-html:** Fix style of code sections ([e883713](https://github.com/zalando-incubator/hexo-theme-doc/commit/e883713))
* **swagger-to-html:** Fix style of code sections ([b3a92a5](https://github.com/zalando-incubator/hexo-theme-doc/commit/b3a92a5))
* **swagger-to-html:** handle format ([5d432b2](https://github.com/zalando-incubator/hexo-theme-doc/commit/5d432b2))
* **swagger-to-md:** hotfix include_module (be sure that will not be overriden) ([7300b0a](https://github.com/zalando-incubator/hexo-theme-doc/commit/7300b0a))
* **swagger-to-md:** respect the order when rendering and pushing the result to the stream ([9002f87](https://github.com/zalando-incubator/hexo-theme-doc/commit/9002f87))


### Features

* add first search implementation  ([94f0ac9](https://github.com/zalando-incubator/hexo-theme-doc/commit/94f0ac9))
* **favicon:** add favicon customization ([a514462](https://github.com/zalando-incubator/hexo-theme-doc/commit/a514462))
* **ga:** add google analytics support ([e2e6cc4](https://github.com/zalando-incubator/hexo-theme-doc/commit/e2e6cc4))
* **swagger-to-md:** add first implementation ([45bddc5](https://github.com/zalando-incubator/hexo-theme-doc/commit/45bddc5))
