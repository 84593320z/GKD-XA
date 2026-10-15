package li.gkd.app.util

/**
 * Max Tokens 自适应策略：全部是纯函数（不碰网络、不碰 Android），便于 JVM 单测。
 *
 * 语义对齐上游 gkd-plus：
 * 1. 生成前若配置值低于 [MIN_TOKENS] 就抬到 [MIN_TOKENS]；
 * 2. 流式响应被截断（finishReason == length）时把预算翻倍重试一次，
 *    但已知上限存在且翻倍后会撞上它时就不再翻倍；
 * 3. 请求被服务端以「max_tokens 过大 / 参数非法」拒绝时回退到 [FALLBACK_TOKENS]，
 *    并把被拒绝的那个值记为该服务商的上限，之后同一服务商不再用它及更大的值。
 *
 * [knownCeiling] 是**开区间**上界（已确认过不去的那道墙），所以预算一律严格小于它。
 */
object AiTokenBudget {

    /** 生成前的最低预算 */
    const val MIN_TOKENS: Int = 16384

    /** 翻倍的绝对上限，任何预算都不超过它 */
    const val CEILING_TOKENS: Int = 131072

    /** 被服务端拒绝后的回退预算 */
    const val FALLBACK_TOKENS: Int = 8192

    /** 本次请求该用的预算：先抬底，再夹到绝对上限与该服务商已知上限之内。 */
    fun budgetForRequest(configured: Int, knownCeiling: Int?): Int =
        clampToCeilings(MIN_TOKENS.coerceAtLeast(configured.coerceAtLeast(1)), knownCeiling)

    /** 输出截断后的翻倍预算；返回 null 表示不再翻倍重试。 */
    fun budgetForTruncatedRetry(current: Int, knownCeiling: Int?): Int? {
        val doubled = current.toLong() * 2L
        if (doubled > CEILING_TOKENS) return null
        if (knownCeiling != null && doubled >= knownCeiling) return null
        return clampToCeilings(doubled.toInt(), knownCeiling)
    }

    /** 被拒绝后回退到的预算：仍受该服务商已知上限约束。 */
    fun budgetForRejection(knownCeiling: Int?): Int = clampToCeilings(FALLBACK_TOKENS, knownCeiling)

    /** 记下的新上限：取「被拒绝的值」与「已记录值」里更小的一个。 */
    fun narrowedCeiling(oldCeiling: Int?, rejectedBudget: Int): Int =
        minOf(oldCeiling ?: rejectedBudget, rejectedBudget)

    /** 判断服务端错误是否属于「max_tokens 过大 / 参数非法」这一类。 */
    fun isTokenLimitRejection(message: String): Boolean {
        val text = message.lowercase()
        if (text.isBlank()) return false
        if (TOKEN_PARAM_KEYS.none { text.contains(it) }) return false
        return LIMIT_ERROR_HINTS.any { text.contains(it) }
    }

    /** 预算夹进 1..CEILING_TOKENS，并在已知上限时严格小于该上限。 */
    private fun clampToCeilings(budget: Int, knownCeiling: Int?): Int {
        val capped = budget.coerceIn(1, CEILING_TOKENS)
        val ceiling = knownCeiling ?: return capped
        return capped.coerceAtMost((ceiling - 1).coerceAtLeast(1))
    }

    private val TOKEN_PARAM_KEYS = listOf(
        "max_tokens",
        "maxtokens",
        "max tokens",
        "max_output_tokens",
        "outputtokens",
        "output tokens",
        "completion_tokens",
        "completion tokens",
    )

    private val LIMIT_ERROR_HINTS = listOf(
        "too large",
        "too big",
        "too high",
        "at most",
        "maximum",
        "must be less",
        "less than",
        "not more than",
        "exceed",
        "greater than",
        "over the limit",
        "invalid",
        "超出",
        "超过",
        "过大",
        "太高",
        "上限",
        "最大",
        "不合法",
        "非法",
    )
}
