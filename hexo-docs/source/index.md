---
title: Overview
slogan: Kotlin ORM lib with SQL DSL
lang: en
related_path: zh-cn/
---

## What's Ktorm?

Ktorm is a lightweight and efficient ORM Framework for Kotlin directly based on pure JDBC. It provides strong typed and flexible SQL DSL and many convenient extension functions to reduce our duplicated effort on database operations. All the SQLs, of course, are generated automatically.

## Features

 - No configuration files, no xml, lightweight, easy to use.
 - Strong typed SQL DSL, exposing low-level bugs at compile time.
 - Flexible query, fine-grained control over the generated SQLs as you wish.
 - Extensible design, write your own extensions to support more operators, data types, SQL functions, etc.
 - Dialects supports, MySQL, Oracle, PostgreSQL, or you can write your own dialect support by implementing the `SqlDialect` interface.

