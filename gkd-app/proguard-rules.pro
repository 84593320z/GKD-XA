# http://developer.android.com/guide/developing/tools/proguard.html
#
# GKD-X 的 R8 规则。
# 说明：绝大部分 keep 规则由各依赖库自带的 consumer-proguard-rules 提供
# （kotlinx-serialization / Room / Shizuku / Ktor / Coil / MIUIX 等）。
# 这里只补充本项目特有的反射、序列化与 JNI 边界。

-dontwarn **

# ---------------------------------------------------------------------------
# 项目自身的入口 / 反射边界
# ---------------------------------------------------------------------------
# Application / Activity / Service 由 manifest 引用，R8 默认已 keep，
# 这里显式保留以避免 AGP 行为差异。
-keep public class li.gkd.app.App { *; }
-keep public class li.gkd.app.MainActivity { *; }

# 无障碍服务、Tile 服务、前台服务由系统通过 manifest 反射实例化
-keep class li.gkd.app.service.** { *; }
-keep class li.gkd.app.a11y.** { *; }

# 特权 / 隐藏 API 探测：内部依赖 Class.forName 与反射字段
-keep class li.gkd.app.priv.** { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep <fields>;
    @androidx.annotation.Keep <methods>;
}

# ---------------------------------------------------------------------------
# 数据模型：@Serializable 的类需要保留伴生对象与序列化器
# （kotlinx-serialization consumer rules 已覆盖大部分，这里针对项目包兜底）
# ---------------------------------------------------------------------------
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,InnerClasses,Signature
-keepclassmembers class li.gkd.app.data.** {
    *** Companion;
}
-keepclasseswithmembers class li.gkd.app.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---------------------------------------------------------------------------
# 第三方库的常见坑位，显式保底（避免因 R8 版本差异导致运行时崩溃）
# ---------------------------------------------------------------------------
# Shizuku 通过 Binder + 反射调用特权 API
-keep class rikka.shizuku.** { *; }
-dontwarn rikka.shizuku.**

# LSPosed HiddenApiBypass 直接操作隐藏 API 反射表
-keep class org.lsposed.hiddenapibypass.** { *; }

# Ktor 使用 ServiceLoader / 反射加载引擎与序列化器
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { volatile <fields>; }
-dontwarn io.ktor.**
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Room 生成的实现类由反射按名字加载
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**
