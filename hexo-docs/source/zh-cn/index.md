---
title: Kotlin ORM 框架
lang: zh-cn
related_path: index.html
---

<p id="logo-full" align="center"><img src="/images/logo-full.png" alt="Ktorm" width="300" /></p><p id="badges" align="center"><a href="https://www.travis-ci.org/vincentlauvlwj/Ktorm"><img src="https://www.travis-ci.org/vincentlauvlwj/Ktorm.svg?branch=master" alt="Build Status" /></a><a href="https://search.maven.org/search?q=g:%22me.liuwj.ktorm%22"><img src="https://img.shields.io/maven-central/v/me.liuwj.ktorm/ktorm-core.svg?label=Maven%20Central" alt="Maven Central" /></a><a href="https://github.com/vincentlauvlwj/Ktorm/blob/master/LICENSE"><img src="https://img.shields.io/badge/license-Apache%202-blue.svg?maxAge=2592000" alt="Apache License 2" /></a><a href="https://app.codacy.com/app/vincentlauvlwj/Ktorm?utm_source=github.com&utm_medium=referral&utm_content=vincentlauvlwj/Ktorm&utm_campaign=Badge_Grade_Dashboard"><img src="https://api.codacy.com/project/badge/Grade/65d4931bfbe14fe986e1267b572bed53" alt="Codacy Badge" /></a><a href="https://www.liuwj.me"><img src="https://img.shields.io/badge/author-vince-yellowgreen.svg" alt="Author" /></a></p>

<style type="text/css">
    #logo-full {
        margin-top: 20px;
    }
    #badges img {
        padding: 0 2px;
    }
</style>

# Ktorm 是什么？

Ktorm 是一个直接基于纯 JDBC 编写的高效简洁的轻量级 Kotlin ORM 框架，它提供了强类型而且灵活的 SQL DSL 和许多方便的扩展函数，以减少我们操作数据库的重复劳动。当然，所有的 SQL 都是自动生成的。

# 特性

 - 没有配置文件、没有 xml、轻量级、简洁易用
 - 强类型 SQL DSL，将低级 bug 暴露在编译期
 - 灵活的查询，随心所欲地精确控制所生成的 SQL
 - 易扩展的设计，可以灵活编写扩展，支持更多数据类型和 SQL 函数等
 - 方言支持，MySQL、Oracle、PostgreSQL，你也可以自己编写方言支持，只需要实现 `SqlDialect` 接口即可