---
title: Ktorm
---

<p id="logo-full" align="center"><a href="https://ktorm.liuwj.me"><img src="/images/logo-full.png" alt="Ktorm" width="300" /></a></p><p id="badges" align="center"><a href="https://www.travis-ci.org/vincentlauvlwj/Ktorm"><img src="https://www.travis-ci.org/vincentlauvlwj/Ktorm.svg?branch=master" alt="Build Status" /></a><a href="https://search.maven.org/search?q=g:%22me.liuwj.ktorm%22"><img src="https://img.shields.io/maven-central/v/me.liuwj.ktorm/ktorm-core.svg?label=Maven%20Central" alt="Maven Central" /></a><a href="https://github.com/vincentlauvlwj/Ktorm/blob/master/LICENSE"><img src="https://img.shields.io/badge/license-Apache%202-blue.svg?maxAge=2592000" alt="Apache License 2" /></a><a href="https://app.codacy.com/app/vincentlauvlwj/Ktorm?utm_source=github.com&utm_medium=referral&utm_content=vincentlauvlwj/Ktorm&utm_campaign=Badge_Grade_Dashboard"><img src="https://api.codacy.com/project/badge/Grade/65d4931bfbe14fe986e1267b572bed53" alt="Codacy Badge" /></a><a href="https://www.liuwj.me"><img src="https://img.shields.io/badge/author-vince-yellowgreen.svg" alt="Author" /></a></p>

<style type="text/css">
    #logo-full {
        margin-top: 20px;
    }
    #badges img {
        padding: 0 2px;
    }
</style>

# What's Ktorm?

Ktorm is a lightweight and efficient ORM Framework for Kotlin directly based on pure JDBC. It provides strong typed and flexible SQL DSL and many convenient extension functions to reduce our duplicated effort on database operations. All the SQLs, of course, are generated automatically.

# Features

 - No configuration files, no xml, lightweight, easy to use.
 - Strong typed SQL DSL, exposing low-level bugs at compile time.
 - Flexible query, exactly control the generated SQLs as you wish.
 - Extensible design, write your own extensions to support more data types, SQL functions, etc.
 - Dialects supports, MySQL, Oracle, PostgreSQL, or you can write your own dialect support by implementing the `SqlDialect` interface.