package li.gkd.app.data

import li.gkd.app.data.RawSubscription.RawAppGroup

/**
 * 把从多个订阅里挑出来的规则组整理成一份可导入的列表。
 *
 * 每个元素带上它来自哪个订阅 (subsId)，因为同一个 key 在不同订阅里是完全不同的组。
 * - 所有组都来自同一个订阅时原样返回：此时 scopeKeys 指向的是目标订阅里已有的组，语义没变。
 * - 跨订阅时才重排冲突的 key，并且只保留能在同一订阅内解析到的 scopeKeys；否则会把
 *   A 订阅的 scopeKey 绑到 B 订阅里同号的组上，是错的。
 */
fun prepareAppGroupsForCopy(groups: List<Pair<Long, RawAppGroup>>): List<RawAppGroup> {
    val firstSubsId = groups.firstOrNull()?.first ?: return emptyList()
    if (groups.all { (subsId, _) -> subsId == firstSubsId }) return groups.map { it.second }
    val usedKeys = mutableSetOf<Int>()
    val copiedKeys = groups.associate { (subsId, group) ->
        val key = if (usedKeys.add(group.key)) {
            group.key
        } else {
            var candidate = requireNotNull(usedKeys.maxOrNull()) + 1
            while (!usedKeys.add(candidate)) {
                candidate++
            }
            candidate
        }
        (subsId to group.key) to key
    }
    return groups.map { (subsId, group) ->
        group.copy(
            key = copiedKeys.getValue(subsId to group.key),
            scopeKeys = group.scopeKeys?.mapNotNull { scopeKey ->
                if (scopeKey == group.key) null else copiedKeys[subsId to scopeKey]
            },
        )
    }
}
