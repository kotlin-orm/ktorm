---
title: Entity Sequence
lang: en
related_path: zh-cn/entity-sequence.html
---

# Entity Sequence

Entity Sequence is a set of query APIs provided by Ktorm for entity objects. With these APIs, we can write code just like using the sequence APIs in Kotlin standard lib, for example:  

```kotlin
val depts = Departments
    .filter { it.location eq "Guangzhou" }
    .sortedBy { it.id }
    .take(2)
    .skip(2)
    .toList()
```

The code above will generate SQL like: 

```sql
select * 
from t_department t
where t.location = ?
order by t.id
limit 2, 2
```

These will be online at Ktorm version 2.x, please stay focused on us!

