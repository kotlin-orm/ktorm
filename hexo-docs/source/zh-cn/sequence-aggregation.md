---
title: 序列聚合
lang: zh-cn
related_path: en/sequence-aggregation.html
---

# 序列聚合

实体序列 API 不仅可以让我们使用类似 `kotlin.Sequence` 的方式获取数据库中的实体对象，它还支持丰富的聚合功能，让我们可以方便地对指定字段进行计数、求和、求平均值等操作。

> 注意：实体序列 API 仅在 Ktorm 2.0 及以上版本中提供。

## 简单聚合

我们首先来看看 `aggregate` 函数的定义：

```kotlin
inline fun <E : Entity<E>, T : Table<E>, C : Any> EntitySequence<E, T>.aggregate(
    aggregationSelector: (T) -> ColumnDeclaring<C>
): C?
```



## 分组聚合