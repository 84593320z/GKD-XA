package li.gkd.app.domain.rule

/** Events broaden cache invalidation; forced-only filtering applies only to forced requests. */
internal data class QueryRequest<R : Any>(
    val byEvent: Boolean,
    val byForced: Boolean,
    val normal: Boolean,
    val delayRules: Set<R>,
) {
    fun merge(other: QueryRequest<R>) = QueryRequest(
        byEvent = byEvent || other.byEvent,
        byForced = byForced && other.byForced,
        normal = normal || other.normal,
        delayRules = delayRules + other.delayRules,
    )
}
