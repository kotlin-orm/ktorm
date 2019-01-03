---
title: Transaction Management
lang: en
related_path: zh-cn/transaction-management.html
---

# Transaction Management

Database transactions allow units of work recover correctlly from failures and keep a database consistent even in cases of system failure, when execution stops and many operations upon a database remain uncompleted, with unclear status. Ktorm provides convenient support for transactions based on JDBC.

## useTransaction function

