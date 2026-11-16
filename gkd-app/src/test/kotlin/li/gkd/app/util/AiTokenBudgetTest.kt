package li.gkd.app.util

import org.junit.Assert.*
import org.junit.Test

class AiTokenBudgetTest {
    @Test fun configuredBudgetIsHonored() {
        assertEquals(4096, AiTokenBudget.budgetForRequest(4096, null))
        assertEquals(1, AiTokenBudget.budgetForRequest(0, null))
        assertEquals(131072, AiTokenBudget.budgetForRequest(Int.MAX_VALUE, null))
        assertEquals(32767, AiTokenBudget.budgetForRequest(65536, 32768))
    }
    @Test fun rejectionConvergesToFourThousandInTwoSteps() {
        val first = AiTokenBudget.budgetForRejection(16384)
        val second = AiTokenBudget.budgetForRejection(first)
        assertEquals(8192, first)
        assertEquals(4096, second)
        assertEquals(2048, AiTokenBudget.budgetForRejection(second))
        assertEquals(1, AiTokenBudget.budgetForRejection(1))
        assertEquals(4096, AiTokenBudget.budgetForRejection(null))
    }
    @Test fun truncationDoesNotReachRejectedBudget() {
        assertEquals(8192, AiTokenBudget.budgetForTruncatedRetry(4096, null))
        assertNull(AiTokenBudget.budgetForTruncatedRetry(4096, 8192))
        assertNull(AiTokenBudget.budgetForTruncatedRetry(131072, null))
        assertEquals(8192, AiTokenBudget.narrowedCeiling(16384, 8192))
    }
    @Test fun minimumAndNonBudgetErrorsNeverReduceBudget() {
        assertTrue(AiTokenBudget.isTokenLimitRejection("max_tokens is too large"))
        assertTrue(AiTokenBudget.isTokenLimitRejection("max_output_tokens exceeds maximum"))
        assertFalse(AiTokenBudget.isTokenLimitRejection("max_tokens below API minimum"))
        assertFalse(AiTokenBudget.isTokenLimitRejection("Incorrect API key"))
        assertFalse(AiTokenBudget.isTokenLimitRejection("Rate limit exceeded"))
    }
}
