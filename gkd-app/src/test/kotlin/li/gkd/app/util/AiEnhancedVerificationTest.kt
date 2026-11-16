package li.gkd.app.util

import kotlinx.coroutines.runBlocking
import li.gkd.app.a11y.lastTriggerRule
import li.gkd.app.a11y.lastTriggerTime
import li.gkd.app.data.AppRule
import li.gkd.app.domain.rule.queryRuleConditions
import li.gkd.selector.NodeAdapter
import org.junit.Assert.*
import org.junit.Test

class AiEnhancedVerificationTest {
    private class Node(var text: String, val children: MutableList<Node> = mutableListOf()) {
        var parent: Node? = null
        init { children.forEach { it.parent = this } }
    }

    private val adapter = object : NodeAdapter<Node>() {
        override fun getAttr(target: Any, name: String): Any? =
            if (target is Node && name == "text") target.text else null
        override fun getName(node: Node) = "Node"
        override fun getChildCount(node: Node) = node.children.size
        override fun getChild(node: Node, index: Int) = node.children.getOrNull(index)
        override fun getParent(node: Node) = node.parent
        override fun getNodeKey(node: Node): Any = node
    }

    private fun rules(props: String = "", extra: String = ""): List<AppRule> = AiEnhancedRules.resolve(
        AiRuleImport.parse("""{id:'app.test',groups:[{key:0,name:'Verify',rules:[{key:1,actionCd:0,matches:'[text="skip"]',$props}$extra]}]}""", "app.test"),
        null, 0,
    )

    private inner class Screen {
        val target = Node("skip")
        val root = Node("root", mutableListOf(target))
        var queries = 0
        var actions = 0
        var clock = 0L
        var validContext = true
        var validWindow = true
        val waits = mutableListOf<Long>()
        var onRefresh: (Node) -> Boolean = { true }
        var isEligible: (AppRule) -> Boolean = { AiEnhancedRules.eligible(it, "app.test", "app.test.Main") }

        fun verifier() = AiEnhancedVerification(
            getRoot = { root },
            matchesContext = { validContext },
            sameWindow = { validWindow },
            refresh = { onRefresh(it) },
            eligible = { isEligible(it) },
            query = { rule, node ->
                queries++
                queryRuleConditions(rule.matches, rule.anyMatches, rule.excludeMatches, rule.excludeAllMatches) {
                    it.match(node, adapter) ?: adapter.querySelector(node, it)
                }
            },
            perform = { _, _ -> actions++; root.children.clear(); true },
            pause = { waits += it; clock += it },
            now = { clock },
        )
    }

    @Test fun recycledTargetAndNewExclusionAreRecheckedAfterRefresh() = runBlocking {
        val recycled = Screen()
        recycled.onRefresh = { node -> if (node === recycled.target) node.text = "purchase"; true }
        assertEquals(AiEnhancedVerification.Result.Unmatched, recycled.verifier().run(rules(), 0))
        assertEquals(2, recycled.queries)
        assertEquals(0, recycled.actions)

        val exclusion = Screen()
        exclusion.onRefresh = { node ->
            if (node === exclusion.root) node.children += Node("protected").also { it.parent = node }
            true
        }
        assertEquals(AiEnhancedVerification.Result.Unmatched, exclusion.verifier().run(rules("excludeMatches:'[text=\"protected\"]'"), 0))
        assertEquals(2, exclusion.queries)
        assertEquals(0, exclusion.actions)
    }

    @Test fun contextWindowRefreshAndPrerequisitesFailClosed() = runBlocking {
        val changed = Screen()
        changed.onRefresh = { changed.validContext = false; true }
        assertEquals(AiEnhancedVerification.Result.ContextChanged, changed.verifier().run(rules(), 0))
        assertEquals(0, changed.actions)
        val window = Screen()
        window.validWindow = false
        assertEquals(AiEnhancedVerification.Result.ContextChanged, window.verifier().run(rules(), 0))
        assertEquals(0, window.actions)
        val failedRefresh = Screen()
        failedRefresh.onRefresh = { false }
        assertEquals(AiEnhancedVerification.Result.ContextChanged, failedRefresh.verifier().run(rules(), 0))
        assertEquals(0, failedRefresh.actions)
        val prerequisite = Screen()
        val dependent = rules(extra = ",{key:2,preKeys:[1],actionCd:0,matches:'[text=\"skip\"]'}").last()
        assertEquals(AiEnhancedVerification.Result.Unmatched, prerequisite.verifier().run(listOf(dependent), 0))
        assertEquals(0, prerequisite.actions)
        assertEquals(0, prerequisite.queries)
    }

    @Test fun matchAndActionDelaysRunBeforeFinalQueryWithoutChangingRuleTiming() = runBlocking {
        val screen = Screen()
        val rule = rules("matchDelay:500,actionDelay:250").single()
        screen.isEligible = {
            assertTrue("eligibility must not be checked before matchDelay", screen.clock >= 500)
            true
        }
        assertEquals(AiEnhancedVerification.Result.Verified, screen.verifier().run(listOf(rule), 0))
        assertEquals(listOf(500L, 250L, 1500L), screen.waits)
        assertEquals(500L, rule.matchDelay)
        assertEquals(250L, rule.actionDelay)
        assertEquals(1, screen.actions)
        val tooLong = Screen()
        assertEquals(AiEnhancedVerification.Result.Unmatched, tooLong.verifier().run(rules("matchDelay:10001"), 0))
        assertEquals(0, tooLong.queries)
        assertEquals(0, tooLong.actions)
        assertTrue(tooLong.waits.isEmpty())
    }

    @Test fun oneShotVerificationDoesNotAlterFormalTriggerHistoryOrRuleCount() = runBlocking {
        val previousRule = lastTriggerRule
        val previousTime = lastTriggerTime
        val formalRule = rules().single()
        val temporaryRule = rules("actionMaximum:1").single()
        try {
            lastTriggerRule = formalRule
            lastTriggerTime = 12345L
            val screen = Screen()
            assertEquals(AiEnhancedVerification.Result.Verified, screen.verifier().run(listOf(temporaryRule), 0))
            assertEquals(1, screen.actions)
            assertSame(formalRule, lastTriggerRule)
            assertEquals(12345L, lastTriggerTime)
            assertTrue("temporary verification must not consume the formal action maximum", temporaryRule.status.ok)
        } finally {
            lastTriggerRule = previousRule
            lastTriggerTime = previousTime
        }
    }
}
