package li.gkd.db

import kotlin.test.Test
import kotlin.test.assertEquals

class DbConvertersTest {
    private val converters = DbConverters()

    @Test
    fun validJsonListRoundTrips() {
        val list = listOf("first", "second")
        assertEquals(list, converters.fromStringToList(converters.fromListStringToString(list)))
    }

    @Test
    fun emptyStringDecodesToEmptyList() {
        assertEquals(emptyList(), converters.fromStringToList(""))
    }

    @Test
    fun malformedJsonIsPreservedAsRawText() {
        // 回归用例：解析失败不得静默吞掉原始日志（上游 601743b9 的核心诉求）
        assertEquals(listOf("{broken"), converters.fromStringToList("{broken"))
        assertEquals(listOf("not-json"), converters.fromStringToList("not-json"))
    }
}
