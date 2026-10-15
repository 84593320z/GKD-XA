package li.gkd.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiTokenBudgetTest {

    @Test
    fun configuredBudgetBelowMinimumIsRaisedToMinimum() {
        assertEquals(16384, AiTokenBudget.budgetForRequest(configured = 4096, knownCeiling = null))
        assertEquals(16384, AiTokenBudget.budgetForRequest(configured = 1, knownCeiling = null))
        assertEquals(16384, AiTokenBudget.budgetForRequest(configured = 0, knownCeiling = null))
    }

    @Test
    fun configuredBudgetAboveMinimumIsKeptButNeverExceedsAbsoluteCeiling() {
        assertEquals(65536, AiTokenBudget.budgetForRequest(configured = 65536, knownCeiling = null))
        assertEquals(131072, AiTokenBudget.budgetForRequest(configured = 999999, knownCeiling = null))
    }

    @Test
    fun knownCeilingIsExclusiveSoTheRejectedValueIsNeverSentAgain() {
        // 16384 被拒绝过：抬底后的 16384 必须让位给严格更小的值
        assertEquals(16383, AiTokenBudget.budgetForRequest(configured = 4096, knownCeiling = 16384))
        assertEquals(32767, AiTokenBudget.budgetForRequest(configured = 65536, knownCeiling = 32768))
        // 记录过的小上限不会被抬底覆盖
        assertEquals(8192, AiTokenBudget.budgetForRequest(configured = 4096, knownCeiling = 8193))
    }

    @Test
    fun truncatedOutputDoublesTheBudgetOnlyOnceBelowEveryCeiling() {
        // budgetForTruncatedRetry 可空，?: -1 让它落进 assertEquals(long, long) 这个重载
        assertEquals(32768, AiTokenBudget.budgetForTruncatedRetry(current = 16384, knownCeiling = null) ?: -1)
        assertEquals(131072, AiTokenBudget.budgetForTruncatedRetry(current = 65536, knownCeiling = null) ?: -1)
        assertNull(AiTokenBudget.budgetForTruncatedRetry(current = 131072, knownCeiling = null))
    }

    @Test
    fun doublingStopsWhenItWouldReachTheKnownProviderCeiling() {
        assertNull(AiTokenBudget.budgetForTruncatedRetry(current = 16384, knownCeiling = 32768))
        assertNull(AiTokenBudget.budgetForTruncatedRetry(current = 16384, knownCeiling = 16384))
        assertEquals(32768, AiTokenBudget.budgetForTruncatedRetry(current = 16384, knownCeiling = 65536) ?: -1)
    }

    @Test
    fun rejectionFallsBackToFallbackBudget() {
        assertEquals(8192, AiTokenBudget.budgetForRejection(knownCeiling = null))
        assertEquals(8192, AiTokenBudget.budgetForRejection(knownCeiling = 16384))
        // 回退值自己也被拒绝过时继续往下让，但始终是个正数
        assertEquals(8191, AiTokenBudget.budgetForRejection(knownCeiling = 8192))
        assertEquals(1, AiTokenBudget.budgetForRejection(knownCeiling = 1))
    }

    @Test
    fun narrowedCeilingKeepsTheTightestKnownWall() {
        assertEquals(16384, AiTokenBudget.narrowedCeiling(oldCeiling = null, rejectedBudget = 16384))
        assertEquals(16384, AiTokenBudget.narrowedCeiling(oldCeiling = 32768, rejectedBudget = 16384))
        assertEquals(16384, AiTokenBudget.narrowedCeiling(oldCeiling = 16384, rejectedBudget = 32768))
    }

    @Test
    fun onlyMaxTokenParameterErrorsAreTreatedAsRejection() {
        assertTrue(
            AiTokenBudget.isTokenLimitRejection(
                "Invalid request: max_tokens is too large: 131072. This model supports at most 65536 completion tokens"
            )
        )
        assertTrue(AiTokenBudget.isTokenLimitRejection("请检查参数 max_output_tokens 超出模型上限"))
        assertTrue(AiTokenBudget.isTokenLimitRejection("Invalid 'max_tokens': integer below API minimum"))
        assertFalse(AiTokenBudget.isTokenLimitRejection(""))
        assertFalse(AiTokenBudget.isTokenLimitRejection("Incorrect API key provided"))
        assertFalse(AiTokenBudget.isTokenLimitRejection("Rate limit exceeded for requests per minute"))
        assertFalse(AiTokenBudget.isTokenLimitRejection("max_tokens 参数正常，但余额不足"))
    }
}
