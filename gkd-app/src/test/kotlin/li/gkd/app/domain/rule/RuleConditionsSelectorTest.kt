package li.gkd.app.domain.rule

import li.gkd.selector.NodeAdapter
import li.gkd.selector.Selector
import org.junit.Assert.*
import org.junit.Test

class RuleConditionsSelectorTest {
    private class Node(val text: String, val children: List<Node> = emptyList()) {
        var parent: Node? = null
        init { children.forEach { it.parent = this } }
    }
    private val ad = Node("Ad")
    private val skip = Node("Skip")
    private val root = Node("Root", listOf(ad, skip))
    private val adapter = object : NodeAdapter<Node>() {
        override fun getAttr(target: Any, name: String): Any? =
            if (target is Node && name == "text") target.text else null
        override fun getName(node: Node) = "Node"
        override fun getChildCount(node: Node) = node.children.size
        override fun getChild(node: Node, index: Int) = node.children.getOrNull(index)
        override fun getParent(node: Node) = node.parent
        override fun getNodeKey(node: Node): Any = node
    }
    private fun query(
        matches: List<String> = listOf("[text='Ad']"),
        any: List<String> = emptyList(),
        exclude: List<String> = emptyList(),
        excludeAll: List<String> = emptyList(),
    ): Node? {
        fun compile(values: List<String>) = values.map { Selector.compile(it).value }
        return queryRuleConditions(compile(matches), compile(any), compile(exclude), compile(excludeAll)) {
            it.match(root, adapter) ?: adapter.querySelector(root, it)
        }
    }

    @Test
    fun excludeAllRequiresEverySelectorToFindANode() {
        assertSame(ad, query(excludeAll = listOf("[text='Skip']", "[text='Missing']")))
        assertSame(ad, query(excludeAll = listOf("[text='Missing']", "[text='Skip']")))
        assertNull(query(excludeAll = listOf("[text='Skip']", "[text='Ad']")))
        assertSame(ad, query(excludeAll = listOf("[text='Missing']")))
        assertSame(ad, query(excludeAll = emptyList()))
    }

    @Test
    fun anyAndAllKeepTheExistingTargetSelectionAndRequirePositiveMatches() {
        assertSame(skip, query(matches = listOf("[text='Ad']", "[text='Skip']")))
        assertSame(skip, query(matches = emptyList(), any = listOf("[text='Missing']", "[text='Skip']")))
        assertNull(query(matches = listOf("[text='Ad']", "[text='Missing']")))
        assertNull(query(any = listOf("[text='Missing']")))
        assertNull(query(matches = emptyList()))
    }

    @Test
    fun excludeAnyRejectsOneHitAndCanFindTheRoot() {
        assertNull(query(exclude = listOf("[text='Missing']", "[text='Skip']")))
        assertNull(query(excludeAll = listOf("[text='Root']", "[text='Skip']")))
        assertSame(root, query(matches = listOf("[text='Root']")))
    }
}
