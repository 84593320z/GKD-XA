package li.gkd.app.util

import li.gkd.app.data.ExcludeData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 包名校验曾经照抄「应用模块包名」的编译规则（至少两段），于是单段包名被当成非法：
 * 系统包 "android" 会被 AppListString.decode 和订阅 exclude 解析静默丢掉。
 */
class AppIdValidationTest {

    @Test
    fun singleSegmentPackageIdsAreValid() {
        assertTrue("android", "android".isValidAppId())
        assertTrue("a", "a".isValidAppId())
        assertTrue("android.settings", "android.settings".isValidAppId())
        assertTrue("com.android.systemui", "com.android.systemui".isValidAppId())
        assertTrue("com.example.my_app2", "com.example.my_app2".isValidAppId())
    }

    @Test
    fun malformedPackageIdsAreStillRejected() {
        assertFalse("空串", "".isValidAppId())
        assertFalse("数字开头", "1android".isValidAppId())
        assertFalse("尾随点", "android.".isValidAppId())
        assertFalse("空段", "android..settings".isValidAppId())
        assertFalse("段首数字", "com.1abc".isValidAppId())
        assertFalse("连字符", "com.example-app".isValidAppId())
        assertFalse("斜杠（activity 写法混进来）", "android/.Settings".isValidAppId())
        assertFalse("中文", "安卓".isValidAppId())
    }

    @Test
    fun appIdListDecodeKeepsSingleSegmentIds() {
        assertEquals(
            setOf("android", "com.android.systemui"),
            AppListString.decode("android\ncom.android.systemui"),
        )
        assertEquals(
            setOf("android"),
            AppListString.decode("android\n\n1bad\ncom..x"),
        )
    }

    @Test
    fun subscriptionExcludeKeepsSingleSegmentIds() {
        assertEquals(setOf("android"), ExcludeData.parse("android").excludeAppIds)
        assertEquals(setOf("android"), ExcludeData.parse("!android").includeAppIds)
    }
}
