---
title: 查询
lang: zh-cn
related_path: en/query.html
---

# 查询

在前面的章节中，我们曾经创建过一个简单的查询，它查询表中所有的员工记录，然后打印出他们的名字，我们的介绍就从这里开始：

````kotlin
for (row in Employees.select()) {
    println(row[Employees.name])
}
````

## Query 对象

