---
title: 概述
slogan: Kotlin ORM 框架
lang: zh-cn
related_path: '/'
layout: home
---

## Ktorm 是什么？

Ktorm 是直接基于纯 JDBC 编写的高效简洁的轻量级 Kotlin ORM 框架，它提供了强类型而且灵活的 SQL DSL 和方便的序列 API，以减少我们操作数据库的重复劳动。当然，所有的 SQL 都是自动生成的。Ktorm 基于 Apache 2.0 协议开放源代码，源码托管在 GitHub，如果对你有帮助的话，请留下你的 star：[vincentlauvlwj/Ktorm](https://github.com/vincentlauvlwj/Ktorm)[![GitHub Stars](https://img.shields.io/github/stars/vincentlauvlwj/Ktorm.svg?style=social)](https://github.com/vincentlauvlwj/Ktorm/stargazers)

## 特性

- 没有配置文件、没有 xml、没有注解、甚至没有任何第三方依赖、轻量级、简洁易用
- 强类型 SQL DSL，将低级 bug 暴露在编译期
- 灵活的查询，随心所欲地精确控制所生成的 SQL
- 实体序列 API，使用 `filter`、`map`、`sortedBy` 等序列函数进行查询，就像使用 Kotlin 中的原生集合一样方便
- 易扩展的设计，可以灵活编写扩展，支持更多运算符、数据类型、 SQL 函数、数据库方言等

## 最新文章

- 2019-08-12 [Ktorm 2.5 发布，支持使用 data class、POJO 或者任意的类型作为实体类](/zh-cn/define-entities-as-any-kind-of-classes.html) <sup class="new-icon">NEW</sup>
- 2019-06-28 [Ktorm - 让你的数据库操作更具 Kotlin 风味](https://www.liuwj.me/posts/ktorm-write-database-operations-in-kotlin-style/)
- 2019-05-04 [你还在用 MyBatis 吗，Ktorm 了解一下？ - 专注于 Kotlin 的 ORM 框架](https://www.liuwj.me/posts/ktorm-introduction/)

