---
title: Overview
slogan: Kotlin ORM lib with SQL DSL
lang: en
related_path: zh-cn/
layout: home
---

## What's Ktorm?

Ktorm is a lightweight and efficient ORM Framework for Kotlin directly based on pure JDBC. It provides strong-typed and flexible SQL DSL and convenient sequence APIs to reduce our duplicated effort on database operations. All the SQLs, of course, are generated automatically. Ktorm is open source under the license of Apache 2.0, and its code can be found on GitHub: [vincentlauvlwj/Ktorm](https://github.com/vincentlauvlwj/Ktorm).

## Features

- No configuration files, no XML, no third-party dependencies, lightweight, easy to use.
- Strong typed SQL DSL, exposing low-level bugs at compile time.
- Flexible queries, fine-grained control over the generated SQLs as you wish.
- Entity sequence APIs, writing queries via sequence functions such as `filter`, `map`, `sortedBy`, etc., just like using Kotlin's native collections and sequences. 
- Extensible design, write your own extensions to support more operators, data types, SQL functions, database dialects, etc.
