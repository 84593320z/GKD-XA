package li.gkd.app.priv

import android.os.IBinder
import priv.kit.core.binder.PrivilegeBinderWrapper

/**
 * 恢复自 GKD-XA-perf.apk / GKD-XA-debug.apk（本地提交 5d3b322）。
 * 原文件 25 行；下列代码按 debug 包 smali 的行号与调用逐条还原，
 * 属性名/可见性/参数顺序均来自 Kotlin metadata，非推测。
 */
class CompatServiceBinder(
    private val serviceName: String,
) {
    @Volatile
    private var cached: IBinder? = null

    fun get(): IBinder {                                    // line 15
        cached?.takeIf { it.pingBinder() }?.let { return it }   // line 15（takeIf/let 为内联，SMAP 指向 fake.kt:26）
        val binder = try {                                  // line 16
            PrivilegeBinderWrapper.fromSystemService(serviceName)   // line 17
        } catch (e: Throwable) {                            // line 18
            throw IllegalStateException("$serviceName 服务 binder 获取失败：${e.message}", e)  // line 19
        } ?: throw IllegalStateException("$serviceName 服务 binder 为空")                      // line 20
        cached = binder                                     // line 21
        return binder                                       // line 22
    }
}
