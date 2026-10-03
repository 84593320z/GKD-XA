# GKD-XA 融合版

基于 [GKD](https://github.com/gkd-kit/gkd) 的 Android 自定义屏幕点击应用分支，界面直接使用 [compose-miuix-ui](https://github.com/compose-miuix-ui/miuix) 的原版 miuix 组件，底层跟随上游新架构。

通过自定义规则，在指定界面满足条件（如屏幕存在特定文字）时，点击节点、位置或执行其他操作。

- **应用**：GKD-XA（`li.songe.gkdx`）
- **界面**：原版 miuix（Scaffold / TopAppBar / NavigationBar / Card / BasicComponent / Preference / Blur / Icons）
- **能力**：上游 GKD 的选择器、订阅规则、快照与自动化能力，并融合 AI 生成规则

## 界面预览（miuix）

| 首页 | 订阅 | 应用 | 设置 |
| :---: | :---: | :---: | :---: |
| ![首页](docs/screenshots/01-home.png) | ![订阅](docs/screenshots/02-subs.png) | ![应用](docs/screenshots/03-apps.png) | ![设置](docs/screenshots/04-settings.png) |

> 悬浮底栏、大标题顶栏、分组卡片与开关等组件来自 [compose-miuix-ui](https://github.com/compose-miuix-ui/miuix)，不再叠加自绘包装层。

## 架构与界面说明

编译底座是上游 GKD 的新模块化架构（包名 `li.gkd.app.*`、Navigation3、Room、Ktor），界面上层**直接使用原版 miuix 组件**。

| | 做法 |
| ---- | ---- |
| **底座** | 跟随上游新架构：模块拆分、Navigation3、Room、Ktor 等全部对齐上游 |
| **界面层** | 只用 `top.yukonga.miuix.kmp.*` 官方组件（ui / preference / icons / blur / navigation3-ui） |
| **业务逻辑** | 使用新底座的实现，不搬运旧代码 |

### 已弃用的 gkd-miuix 界面方案

早期版本把旧版 MIUIX 渲染层「翻译」到新底座上（自绘 `Perf*` / `Gk*` 包装层 + 自研液态玻璃底栏），
这套方案 bug 偏多（底栏偶发变黑、转场后毛玻璃断档、按压高亮露出尖角、内联图标占位符崩溃等），
现已整体弃用：

- 首页外壳改为「一个 `Scaffold` + 一个 `LayerBackdrop`」，模糊统一交给 miuix 官方 `Modifier.textureBlur`
- 删除自研液态玻璃底栏（`ui/liquid` 整包）与配套的分页采样互斥 / 离屏栅格化 hack
- 删除 `GkTriStateSwitch`（691 行自绘开关）、`GkSubsItemCard`、`GkAuthCard`、`GkAuthButtonGroup`、`EmptyText` 等无人引用的残留组件
- 点击 / 按压反馈回归 miuix `Card` 内部，由 `squircleSurface` 统一裁剪
- 内联图标占位符改用统一的 `TextStyle.placeholderWidth()/placeholderHeight()` 兜底

## 相对旧版的功能变化

> 旧版 = 重构前的 MIUIX 版本；新版 = 当前 GKD-XA。

### 新增 / 增强

| 功能 | 说明 |
| ---- | ---- |
| **AI 生成规则（多服务商）** | 新增「AI 服务商」列表页，可同时保存多套协议 / 地址 / 密钥 / 模型池，点选圆点即切换当前服务商 |
| **服务商详情页签** | 拆成「配置 / 模型」两页：Base URL、API Key（可显隐）、端点模式（Chat Completions / Responses）、`anthropic-version`、自定义请求头、系统提示词与生成参数 |
| **模型管理** | 支持远端拉取并按 Model ID 合并、关键字搜索、多选批量删除、逐个编辑上下文长度与思考标记 |
| **测试连接** | 直接读取远端 `/models` 并把返回模型并入列表，结果行内即时反馈 |
| **自动迁移** | 首次启动把旧的单份 AI 配置迁移为一个服务商，无需重填 |
| **通知文案自定义** | 主标题 / 副标题 / 正文均可用模板变量（含规则数、应用数、触发次数等），并带实时预览 |
| **触发提示样式** | 支持悬浮窗 / Toast / 流体云与灵动岛实时通知等 |
| **规则类别** | 类别前缀匹配、跟随订阅或规则组默认值、批量设置与冲突检测 |
| **控制关系图** | 可视化当前规则组的开关来源（订阅 / 类别 / 应用 / 自身设置） |
| **局部无线调试** | 基于自有特权运行时（priv-kit），不依赖外部授权器 |

### 修复记录

- **修复首页「应用」数量不准确**：该卡片误用订阅快照表的 `size`（等于订阅条数），改为与「应用」Tab 同源的已安装应用列表
- **修复应用列表点击闪退**：miuix 的 `TextStyle`（`body1` / `body2` …）默认只声明 `fontSize`，`lineHeight` 是 `TextUnit.Unspecified`，而 Compose 的 `Placeholder` 不接受 Unspecified。含全局规则组的应用、以及系统应用，进入应用配置页必崩；现已在 `GkGroupNameText` / `GkAppNameText` / `GroupNameText` 统一兜底
- **修复订阅卡片按压高亮露出尖角**：点击反馈曾挂在卡片外层的 `combinedClickable` 上，涟漪绘制在卡片背景之下且不受 squircle 裁剪，只从四个圆角溢出；现已移入卡片内部
- **修复首页右上角火箭按钮闪退**：`TextStyle` 的 `lineHeight` / `fontSize` 不是 `Sp` 单位时直接 `toDp()` 会抛 `IllegalStateException: Only Sp can convert to Px`。新增安全工具 `TextUnit.toSpDpOr()` / `TextStyle.lineHeightDp()` 兜底
- **修复订阅卡片语义**：恢复 `selected` / `Role.Checkbox` / `onClick(label)` / `onLongClick(label)` 无障碍语义；长按已选项不再误取消（`select` 而非 `toggle`）
- **修复订阅页丢失全选 / 反选**：补回批量操作入口
- **修复局部关闭「继续」按钮**：修正 scope 取消导致的点击无响应
- **修复通知文案静默改开关**：条件收集改为无条件前置收集，避免切换开关时改变 composable 调用点数量
- **修复二级页顶栏风格割裂**：补回 `GkPageScaffold` 毛玻璃顶栏（原底座为纯色实心，与首页不一致）

## 免责声明

**本项目遵循 [GPL-3.0](/LICENSE) 开源，仅供学习交流，禁止用于商业或非法用途。**

上游 GKD 项目声明同样适用，请遵守当地法律法规。

## 与上游的关系

| 项目 | 说明 |
| ---- | ---- |
| 本仓库 | [84593320z/gkd-](https://github.com/84593320z/gkd-) |
| 直接来源 | [hanchuan8/gkd-miuix](https://github.com/hanchuan8/gkd-miuix)（本融合版在其基础上开发） |
| 上游 | [gkd-kit/gkd](https://github.com/gkd-kit/gkd) |
| AI 生成规则 | 移植自 [fjjzy/gkd-plus](https://github.com/fjjzy/gkd-plus)（GPL-3.0） |
| 文档 / 选择器说明 | 仍可参考 <https://gkd.li> |
| 订阅规则 | 兼容 GKD 订阅格式，可使用社区订阅 |

## 安装

从本仓库 [Releases](https://github.com/84593320z/gkd-/releases) 下载安装包。

也可自行编译：

```bash
./gradlew :gkd-app:assembleGkdRelease
```

如遇规则 / 选择器问题，可先查阅上游 [疑难解答](https://gkd.li/guide/faq)。

## 构建类型

| 类型 | 包名 | 说明 |
| ---- | ---- | ---- |
| `debug` | `li.songe.gkdx.debug` | 调试版，应用名带 `Debug` 后缀 |
| `perf` | `li.songe.gkdx` | 性能实测版：启用 R8 优化与资源压缩、`isDebuggable=false`；签名使用当前 GKD 签名配置 |
| `release` | `li.songe.gkdx` | 发布版，开启 R8 混淆与资源压缩 |

日常测试建议用 `perf`：

```bash
./gradlew :gkd-app:assembleGkdPerf
```

## 开源致谢

本项目在 [GKD](https://github.com/gkd-kit/gkd)（GPL-3.0）与 [hanchuan8/gkd-miuix](https://github.com/hanchuan8/gkd-miuix)（GPL-3.0）基础上开发，并使用了下列开源项目（不完全列表）：

| 项目 | 说明 | 链接 |
| ---- | ---- | ---- |
| **compose-miuix-ui** | MIUIX 风格 Compose 组件 / Preference / Icons / Blur | [compose-miuix-ui/miuix](https://github.com/compose-miuix-ui/miuix) |
| **GKD** | 核心自动化、选择器与订阅能力 | [gkd-kit/gkd](https://github.com/gkd-kit/gkd) |
| **gkd-miuix** | 本融合版的直接来源 | [hanchuan8/gkd-miuix](https://github.com/hanchuan8/gkd-miuix) |
| **gkd-plus** | AI 生成规则与 AI 配置入口 | [fjjzy/gkd-plus](https://github.com/fjjzy/gkd-plus) |
| Jetpack Compose | UI 框架 | [androidx/compose](https://developer.android.com/jetpack/compose) |
| AndroidX / Room | 应用基础组件与本地数据库 | [AndroidX](https://developer.android.com/jetpack) |
| Ktor | 网络请求 | [ktorio/ktor](https://github.com/ktorio/ktor) |
| Shizuku | 特权 API 调用 | [RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku) |
| XXPermissions | 权限请求 | [getActivity/XXPermissions](https://github.com/getActivity/XXPermissions) |
| Toaster | Toast | [getActivity/Toaster](https://github.com/getActivity/Toaster) |
| DeviceCompat | 设备兼容 | [getActivity/DeviceCompat](https://github.com/getActivity/DeviceCompat) |
| compose-webview | WebView | [KevinnZou/compose-webview](https://github.com/KevinnZou/compose-webview) |
| reorderable | 列表拖拽排序 | [Calvin-LL/Reorderable](https://github.com/Calvin-LL/Reorderable) |

完整依赖与版本见 [`gradle/libs.versions.toml`](gradle/libs.versions.toml)。各自许可证以原项目为准。

## 订阅

默认不内置规则，需自行添加本地规则或通过订阅链接获取远程规则。

第三方订阅可参考：<https://github.com/topics/gkd-subscription>

## 反馈

问题与建议请提交到本仓库 Issues：

<https://github.com/84593320z/gkd-/issues>
