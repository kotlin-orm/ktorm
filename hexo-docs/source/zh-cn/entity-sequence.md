---
title: 实体序列
lang: zh-cn
related_path: en/entity-sequence.html
---

# 实体序列

实体序列是 Ktorm 针对实体对象提供的类似于 `kotlin.Sequence` 的查询 API，它使用起来就像在调用 Kotlin 标准库中的序列 API 一样。例如：

```kotlin
val depts = Departments
    .filter { it.location eq "Guangzhou" }
    .sortedBy { it.id }
    .take(2)
    .skip(2)
    .toList()
```

这段代码会生成以下 SQL：

```sql
select * 
from t_department t
where t.location = ?
order by t.id
limit 2, 2
```

此功能将在 Ktorm 2.x 版本中提供，敬请期待！

