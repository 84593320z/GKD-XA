package li.gkd.app.domain.rule

/** The shared production combination policy; query performs the real selector lookup. */
internal fun <S, N : Any> queryRuleConditions(
    matches: List<S>,
    anyMatches: List<S>,
    excludeMatches: List<S>,
    excludeAllMatches: List<S>,
    query: (S) -> N?,
): N? {
    var target: N? = null
    if (anyMatches.isNotEmpty()) {
        target = anyMatches.firstNotNullOfOrNull(query) ?: return null
    }
    for (selector in matches) target = query(selector) ?: return null
    if (excludeMatches.any { query(it) != null }) return null
    if (excludeAllMatches.isNotEmpty() && excludeAllMatches.all { query(it) != null }) return null
    return target
}
