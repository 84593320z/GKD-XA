package li.gkd.app.util

import li.gkd.app.data.RawSubscription
import org.junit.Assert.*
import org.junit.Test

class AiRuleImportTest {
    private val valid = """{id:'app.test',groups:[{key:0,name:'AI one',scopeKeys:[0,1],activityIds:['.Main'],excludeActivityIds:['.Blocked'],rules:[{key:10,matches:['[text="skip"]','[clickable=true]'],anyMatches:['[id="close"]'],excludeMatches:['[text="safe"]'],excludeAllMatches:['[text="a"]','[text="b"]']}]},{key:1,name:'AI two',scopeKeys:[0],rules:[{key:20,preKeys:[10],actionCdKey:10,matches:'[text="close"]'}]}]}"""

    @Test fun actualImportRetainsConditionsAndRemapsSelfAndPeerGroups() {
        val generated = AiRuleImport.parse(valid, "app.test")
        val current = RawSubscription.parse("""{id:-2,name:'Local',version:0,apps:[{id:'app.test',groups:[{key:0,name:'Existing',rules:[]}]}]}""")
        val result = AiRuleImport.merge(current, generated)
        val groups = result.apps.single().groups
        assertEquals(listOf(0,1,2), groups.map { it.key })
        assertEquals(listOf(1,2), groups[1].scopeKeys)
        assertEquals(listOf(1), groups[2].scopeKeys)
        assertEquals(listOf(10), groups[2].rules.single().preKeys)
        assertEquals(10, groups[2].rules.single().actionCdKey)
        assertEquals(listOf(".Main"), groups[1].activityIds)
        assertEquals(listOf(".Blocked"), groups[1].excludeActivityIds)
        val rule = groups[1].rules.single()
        assertEquals(2, rule.matches!!.size)
        assertEquals(1, rule.anyMatches!!.size)
        assertEquals(1, rule.excludeMatches!!.size)
        assertEquals(2, rule.excludeAllMatches!!.size)
        assertThrows(IllegalStateException::class.java) { AiRuleImport.merge(result, generated) }
    }

    @Test fun aiBatchNamespaceAvoidsOverflowAndNeverBindsExistingGroups() {
        val generated = AiRuleImport.parse(valid, "app.test")
        val current = RawSubscription.parse("""{id:-2,name:'Local',version:0,apps:[{id:'app.test',groups:[{key:0,name:'Existing zero',rules:[]},{key:2147483647,name:'Existing max',rules:[]},{key:-2147483648,name:'Existing min',rules:[]}]}]}""")
        val groups = AiRuleImport.merge(current, generated).apps.single().groups
        assertEquals(5, groups.map { it.key }.distinct().size)
        assertEquals(Int.MIN_VALUE + 1, groups[3].key)
        assertEquals(1, groups[4].key)
        assertEquals(listOf(groups[3].key, groups[4].key), groups[3].scopeKeys)
        assertEquals(listOf(groups[3].key), groups[4].scopeKeys)
        assertEquals(listOf(10), groups[4].rules.single().preKeys)
        assertEquals(10, groups[4].rules.single().actionCdKey)
    }

    @Test fun wrongAppEmptyInvalidDuplicateAndUnboundRulesCannotImport() {
        val invalid = listOf(
            valid.replace("app.test", "other.app"),
            """{id:'app.test',groups:[]}""",
            """{id:'app.test',groups:[{key:0,name:'Empty',rules:[]}]}""",
            """{id:'app.test',groups:[{key:0,name:'Bad',rules:[{matches:'['}]}]}""",
            """{id:'app.test',groups:[{key:0,name:'Duplicate',rules:[{key:1,matches:'*'},{key:1,matches:'*'}]}]}""",
            valid.replace("scopeKeys:[0,1]", "scopeKeys:[99]"),
            valid.replace("preKeys:[10]", "preKeys:[99]"),
            """{apps:[],globalGroups:[{key:0,name:'Global',rules:[]}]}""",
            """{apps:[{id:'app.test',groups:[]},{id:'other.app',groups:[]}]}""",
        )
        invalid.forEach { text -> assertThrows(Exception::class.java) { AiRuleImport.parse(text, "app.test") } }
    }

    @Test fun normalizedNumericRuleKeysCannotSilentlyDropRules() {
        for (key in listOf("'1'", "'01'", "1")) {
            val text = """{id:'app.test',groups:[{key:0,name:'Duplicate',rules:[{key:1,matches:'*'},{key:$key,matches:'*'}]}]}"""
            assertThrows(Exception::class.java) { AiRuleImport.parse(text, "app.test") }
        }
    }

    @Test fun failedRefinementDoesNotDiscardLatestValidatedCandidate() {
        val candidate = AiRuleCandidate("app.test")
        val good = candidate.accept(valid)
        assertNotNull(good)
        assertNull(candidate.accept("not json"))
        assertSame(good, candidate.latest)
        assertNull(candidate.accept(valid.replace("app.test", "other.app")))
        assertSame(good, candidate.latest)
    }

}
