# GKD-XA v1.5.2 修复验收

基线：`87602f514934cb5204286489352ede4083211128`。版本：1.5.2 / 153。日期：2026-11-16。

原审查 F01–F22、复核 N01–N05 及相关存储、调试、安装、启动风险已完成代码修复。修复后的五个独立复核范围（AI、核心、设置与存储、安装与恢复、HTTP 与发布）均未保留有证据支持的开放代码问题。统一 JVM 回归共 461 项通过，Python 发布与 SQL 回归 7 项通过；受控浏览器完成本地调试台主要交互验证。优化 APK 已通过 R8、资源优化和 lintVital 构建。本报告记录推送前验收，正式发布产物由标签流水线重新构建、签名并校验。以上不等于真机、ROM、真实 AI 服务商或实际安装升级验收。

## 范围与使用限制

授权与排除项见[审查范围](<audit-scope.md>)。修复以仓库所有者提供的原报告、复核确认结果和修复期间可复现的回归为依据。未读取真实用户快照或凭据，未将测试用合成数据之外的内容提交到仓库。此报告采用普通源代码修复报告结构，不将本项目描述为恶意样本。

升级前应将完整备份保存在应用之外。首次启动迁移旧数据到私有目录，失败时停止配置和数据库初始化，并提供重试。旧版不能直接读取新目录；Android 降级安装、签名和数据兼容性仍有系统限制。诊断包具有行数、文本与总量上限，并明确标识省略内容，不能代替完整备份。

## 修复映射

下表状态为“源码与对应自动化边界关闭”；confidence=high，status=validated（修复行为），severity 沿用原问题等级。设备结果不在这一状态中。

| 原编号 / 原等级 | 最终行为与主要位置 | 证据 |
|---|---|---|
| F01 / P1 | 增强验证复用正式匹配与限制，保留 AND/OR、排除、app/activity、前置和节点上下文；动作前后复查场景。[增强验证](<../gkd-app/src/main/kotlin/li/gkd/app/util/AiEnhancedVerification.kt>) | E01、E02 |
| F02 / P1 | 导入前严格解析设置、规则与数据库；坏输入不开始提交，不覆盖健康配置。[备份管理](<../gkd-app/src/main/kotlin/li/gkd/app/data/backup/BackupManager.kt>) | E01、E02 |
| F03 / P1 | 结构化递归脱敏 name/value、自定义认证头；排除恢复文件和任意二进制，预算先于编码。[诊断](<../gkd-app/src/main/kotlin/li/gkd/app/data/backup/DiagnosticFiles.kt>) | E01、E02、E04 |
| F04 / P2 | 事件队列只消费连续相同页面批次，保持顺序与所有权释放。[队列](<../gkd-app/src/main/kotlin/li/gkd/app/domain/rule/OwnedEventQueue.kt>) | E01、E02 |
| F05 / P2 | 手势接受、成功、取消、超时分别处理；只有成功回调提交状态，旧周期不回填新周期。[动作](<../gkd-app/src/main/kotlin/li/gkd/app/a11y/RuleAction.kt>) | E01、E02 |
| F06、F07 / P2 | 支持 Responses output_text，Anthropic 跳过 thinking 并合并所有 text 块。[协议](<../gkd-app/src/main/kotlin/li/gkd/app/util/AiTransport.kt>) | E01、E02 |
| F08 / P2 | 流式读取，限制总时长、空闲时间和响应大小，取消释放连接。[协议](<../gkd-app/src/main/kotlin/li/gkd/app/util/AiTransport.kt>) | E01、E02 |
| F09 / P2 | 拉取模型只要求服务商连接参数，不要求预选模型。[模型页](<../gkd-app/src/main/kotlin/li/gkd/app/feature/settings/ai/AiProviderModelsTab.kt>) | E01 |
| F10、F11 / P2 | 配置草稿保留后续模型变更，自定义头删除和改名不会留下旧项。[草稿](<../gkd-app/src/main/kotlin/li/gkd/app/feature/settings/ai/AiProviderDraft.kt>) | E01、E02 |
| F12 / P2 | 普通与增强任务统一有界调度，缺配置、失败、取消均释放 busy/计数。[调度](<../gkd-app/src/main/kotlin/li/gkd/app/util/AiTaskScheduler.kt>) | E01、E02 |
| F13 / P2 | 合并组重排 key 时重映 scopeKeys，保留 rule key 引用语义；拒绝规范化后重复数字 key。[导入](<../gkd-app/src/main/kotlin/li/gkd/app/util/AiRuleImport.kt>) | E01、E02 |
| F14 / P2 | 后续解析失败仍保留可保存的合法候选，保存提示以实际落盘结果为准。[生成](<../gkd-app/src/main/kotlin/li/gkd/app/util/AiRuleGenerator.kt>) | E01、E02 |
| F15 / P2 | 校验实际 APK 包名、当前签名、更高 versionCode、预期版本与可选摘要；不从版本名伪造 code。[更新身份](<../gkd-app/src/main/kotlin/li/gkd/app/util/UpdateApkMetadata.kt>) | E01、E02、E04 |
| F16 / P2 | 日志由串行后台 writer 写入；排空屏障与历史失败分离，短暂失败可恢复。[日志写入](<../gkd-app/src/main/kotlin/li/gkd/app/data/backup/SerialLogWriter.kt>) | E01、E02 |
| F17 / P2 | 递归复制选定快照，按快照 ID 日期窗口筛选，迁移目录 mtime 不扩大范围。[诊断](<../gkd-app/src/main/kotlin/li/gkd/app/data/backup/DiagnosticFiles.kt>) | E01、E02、E04 |
| F18 / P2 | 查询退出与 pending 唤醒协调，旧工作结束时不遗漏新请求。[唤醒](<../gkd-app/src/main/kotlin/li/gkd/app/domain/rule/QueryWakeup.kt>) | E01、E02 |
| F19 / P2 | 跨进程持久化崩溃窗口与显式恢复；单一 crash writer 有界等待，次级异常不能抢先终止。[崩溃门禁](<../gkd-app/src/main/kotlin/li/gkd/app/priv/CrashHandlerGate.kt>) | E01、E02 |
| F20 / P2 | 输出预算按服务商、端点、模型学习，低上限回退可收敛。[预算](<../gkd-app/src/main/kotlin/li/gkd/app/util/AiTokenBudget.kt>) | E01、E02 |
| F21 / P2 | 拒绝空规则、非法 selector、多 app/global 及错误应用；保存计数以实际导入为准。[导入](<../gkd-app/src/main/kotlin/li/gkd/app/util/AiRuleImport.kt>) | E01、E02 |
| F22 / P3 | 运行期间隔用单调时钟，业务日期保留墙钟；未触发冷却和首次刷新用 nullable 状态。[规则状态](<../gkd-app/src/main/kotlin/li/gkd/app/data/ResolvedRule.kt>) | E01、E02 |
| N01 / P2 | excludeAllMatches 仅全部条件命中时排除，保留空集合边界。[条件](<../gkd-app/src/main/kotlin/li/gkd/app/domain/rule/RuleConditions.kt>) | E01、E02 |
| N02 / P2 | 生成结果绑定可信快照的 expectedAppId。[导入](<../gkd-app/src/main/kotlin/li/gkd/app/util/AiRuleImport.kt>) | E01、E02 |
| N03 / P2 | 普通和增强任务共用队列，调用方资源维持到实际任务结束。[调度](<../gkd-app/src/main/kotlin/li/gkd/app/util/AiTaskScheduler.kt>) | E01、E02 |
| N04 / P2 | 删除 AI 请求、响应与错误正文日志，只记录元信息和固定安全错误信息。[生成](<../gkd-app/src/main/kotlin/li/gkd/app/util/AiRuleGenerator.kt>) | E01、E02 |
| N05 / P2 | 受保护认证头大小写无关去重并设置唯一值。[协议](<../gkd-app/src/main/kotlin/li/gkd/app/util/AiTransport.kt>) | E01、E02 |
| R1、R2、C02 / 条件风险 | 本地调试会话鉴权与 Host/Origin 检查；私有存储迁移及旧/新系统备份排除规则。[HTTP](<../gkd-app/src/main/kotlin/li/gkd/app/service/DebugHttpSecurity.kt>)、[迁移](<../gkd-app/src/main/kotlin/li/gkd/app/data/backup/PrivateStorageMigration.kt>)、[Manifest](<../gkd-app/src/main/AndroidManifest.xml>) | E01、E02、E03 |
| R4、C01 / 条件风险 | 异步事件复制与释放；阻塞调用有界并发；安装等待、断连、取消和晚终态由进程 registry 持有。[安装会话](<../gkd-app/src/main/kotlin/li/gkd/app/priv/InstallSessionRegistry.kt>) | E01、E02 |
| C03、C04 / 条件风险 | 首次持久化不跳过；完整备份保持配置/DB/订阅一致捕获；提交后刷新失败单独返回。[文件状态](<../gkd-app/src/main/kotlin/li/gkd/app/store/FileStateStore.kt>)、[备份](<../gkd-app/src/main/kotlin/li/gkd/app/data/backup/BackupManager.kt>) | E01、E02 |
| R3 / 待设备确认 | 节点页布局与圈选实现已审阅，未把未经真机复现的坐标偏移当成已证实缺陷。设备验收仍保留。[节点页](<../gkd-app/src/main/kotlin/li/gkd/app/feature/snapshot/SnapshotNodesPage.kt>) | E01 |

## 修复后复核发现的回归

- CORE-01/02/03：修正首次刷新 sentinel、长冷却初值及迟到动作污染重置状态；共享 cooldown/count 分别记录 epoch，同锁条件提交，保留 App/Match/Activity 策略与成功导航动作的历史记录。
- LF-01/02：安装晚 Failure 清理对应 pending 并释放重启抑制，Success/unknown 保留；句柄身份隔离重复/复用 session ID。首个崩溃处理器独占记录和终止，后续异常不能提前杀进程。
- R-S1/2/3/4：快照 ID 日期窗口、SQL/配置/编码预算前置、日志短暂失败后可排空、迁移残留目录回收前确认存在另一份完整副本。
- R-S5：截断嵌套 JSON 会破坏递归凭据脱敏。生产 SQL 现在对超过 1024 字符的值输出完整合法遗漏标记，短值和 NULL 原样保留。回归覆盖 256 KiB 嵌套转义凭据、9 条 SQL 以及真实 DTO→编码→脱敏→文件路径。
- 统一测试整合：延迟初始化依赖存储的活动流，保留私有存储门禁；修正 JUnit 非 void 方法、测试客户端 Host/实际请求体，以及 Windows 原子替换窗口的等待断言。没有删除失败测试或放宽最终行为断言。

## 验证证据

| ID | 来源 / 可复现入口 | 结果与边界 | content_hash |
|---|---|---|---|
| E01 | 当前提交的生产源码及五个范围的独立复核 | 全部指定已确认问题关闭；复核记录由维护者保留，源码以本提交为准 | n/a（Git 对象提供版本身份） |
| E02 | 下方 Gradle 命令；[应用测试](<../gkd-app/src/test/kotlin/li/gkd/app>)、[选择器测试](<../gkd-selector/src>)、[数据库测试](<../gkd-db/src/jvmTest>) | 应用350、选择器90、数据库21；0失败、0错误、0跳过 | n/a（由本提交重建） |
| E03 | Playwright 1.65.1 / Edge，实际本地控制台 HTML 与 CSP，受控同源 API | 登录、fragment 清理、HttpOnly cookie、刷新、查询/筛选/截图/删除/动作/订阅、文本安全渲染、RPC错误、过期和未授权通过；24请求。未连接 Android HTTP 服务 | n/a（本机验证记录） |
| E04 | [Python回归](<../scripts/test_release_metadata.py>)、[SQL回归入口](<../scripts/test_diagnostic_sql.py>) | 7项通过；签名/身份/大小错误拒绝，实际字节摘要与渠道回填；9条实际SQLite查询行数/文本/分页/时间范围 | n/a（由本提交重建） |
| E05 | 本地 gkdPerf 优化 APK；[正式签名工作流](<../.github/workflows/Build-Release.yml>) | BUILD SUCCESSFUL，5m1s；5,958,835字节，包名li.songe.gkdx，code153。未打标签的本地产物版本带基线提交后缀，只作构建验证；正式产物以标签发布清单为准 | b1f70a1bf6e227fc3740e1ba7e6e5fc922ebdf00d66242cb263385e3ff21dee2 |

复现需要 JDK 21、项目固定 Gradle/Android SDK、Python 3.11+，在仓库根目录执行：

```bash
python3 -B -m unittest discover -s scripts -p "test_*.py"
./gradlew gkd-app:testGkdDebugUnitTest gkd-selector:jvmTest gkd-db:jvmTest gkd-app:assembleGkdPerf --stacktrace
python3 scripts/release_metadata.py validate --tag v1.5.2
```

## 调用路径

P01（callflow，F03/N04/R-S5，E01/E02/E04）：事件/配置进入数据库 → SQL先限制行数并将超长值整体省略 → DiagnosticBudget 在查询/编码前检查预算 → DiagnosticFiles 对完整 JSON 递归脱敏 → 写入诊断临时目录并压缩。回归同时证明限额与秘密值不出现在最终文件；普通页面内容和截图仍可能含私人信息，用户导出后应自行检查再分享。

P02（callflow，F15/R4/LF-01，E01/E02/E04）：更新元数据 → 有界下载与摘要 → APK包名/签名/版本检查 → 完整安全备份 → 全局串行安装会话 → 回调先结算进程registry再通知等待者。没有明确失败的超时/断连不能启动第二次安装；晚失败不会自行弹出系统安装器。

P03（callflow，F05/F22/CORE-03，E01/E02）：引擎捕获规则周期 → 验证当前状态 → 等待平台动作结果 → 成功时同锁按规则及共享cell epoch提交次数/冷却 → 保留实际成功动作日志与原页面信息。已提交到系统的操作不能用协程取消视为已撤销。

## 时间线与剩余设备边界

2026-11-16：确认原22项与新增5项 → 分域修复并补生产helper回归 → 首轮独立复核发现并修正核心、安装、诊断增量 → 统一构建修正Room保留字别名与测试整合问题 → 第二轮独立复核关闭R-S5 → 461项JVM及7项Python回归通过 → 优化APK构建通过，允许进入远端CI和标签签名发布。

下列内容未在 Android 设备上验证：旧Android事件回收和真实节点树、手势回调与页面切换；Root/ADB/Shizuku、Binder死亡、系统确认页、OEM进程替换和FGS；文件系统同步、真实大库迁移、系统备份transport、备份恢复全链；节点圈选的旋转/状态栏/截图比例；真实AI服务商的SSE/取消/兼容行为。安装回调等待有业务期限，不能据此声称同步Binder全流程能硬中断。JVM和浏览器通过不消除这些验证边界。
