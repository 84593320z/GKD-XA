# 更新内容

## 未发布

**修复 + 界面回归原版 miuix**

- 修复首页「应用」数量不准确：该卡片误用订阅快照表的 `size`（等于订阅条数），改为与「应用」Tab 同源的已安装应用列表
- 修复应用列表点击后闪退：miuix 的 `TextStyle`（`body1` / `body2` …）默认只声明 `fontSize`，`lineHeight` 为 `TextUnit.Unspecified`，而 Compose 的 `Placeholder` 不接受 Unspecified。含全局规则组的应用、以及系统应用，进入应用配置页必崩；现已在标题/规则组名称的内联图标占位符处统一兜底
- 修复订阅卡片点按后按压高亮露出四个尖角：点击反馈曾挂在卡片外层的 `combinedClickable` 上，涟漪绘制在卡片背景之下且不受 squircle 裁剪，只从四个圆角溢出；现已移入卡片内部
- 修复二级页顶栏上方大片留白：MIUIX 的 `TopAppBar` 是**大标题**栏，标题传空串时仍会占掉一整行大标题高度（`CollapsedHeight` + 一行 `title1` + `LargeTitleBottomPadding`），再把 `bottomContent` 顶到更下面。应用配置页、订阅应用列表页这类「标题自带渲染（应用名内联图标 / 已选 N 项）」的顶栏现改用 `SmallTopAppBar`，标题紧贴图标行下方，约省掉 34dp 空白
- 弃用早期移植自 gkd-miuix 的界面方案，改为直接使用原版 miuix 组件：
  - 首页外壳简化为「一个 `Scaffold` + 一个 `LayerBackdrop`」，模糊统一走 miuix 官方 `Modifier.textureBlur`
  - 删除自研液态玻璃底栏（`ui/liquid` 整包）与配套的分页采样互斥、离屏栅格化 hack，设置页同步移除「液态玻璃」开关
  - 删除 `GkTriStateSwitch`（691 行自绘开关）、`GkSubsItemCard`、`GkAuthCard`、`GkAuthButtonGroup`、`EmptyText` 等无人引用的残留组件
- 界面层继续把残留的 Material3 组件换成原版 miuix：
  - 对话框/操作按钮：`TextButton` 39 处（19 个文件）改用 miuix `TextButton`，确认类按钮统一为 `textButtonColorsPrimary()`，破坏性操作沿用红色
  - 图标按钮配色来源统一（`LocalContentColor` 20 个文件改读 miuix 主题值，与 miuix `Card` / `BasicComponent` 的内容色对齐）
  - 加载指示：`CircularProgressIndicator` / `LinearProgressIndicator` 改用 miuix 版本
  - 容器与分割线：`Surface`、`HorizontalDivider` 改用 miuix 版本
  - 应用配置页顶栏标题改为居中显示
  - 图标按钮：`GkIconButton` / `GkSizedIconButton` / `GkBlockCloseIconButton` / `GkSearchCloseIconButton` 与三处直用的 `IconButton` 全部改用 miuix `IconButton`（miuix 无 `colors` 参数，配色改为 `backgroundColor` + 图标 `tint`）；自绘的 `CustomIconButton`（Box + Material ripple）删除
- 应用配置页整理：
  - 去掉「本地订阅 >」分组入口（它指向的二级订阅页功能与本页重复）；单订阅时整行不再显示，仅当同一应用被多份订阅覆盖时才保留一个不可点的分组标签以便区分来源
  - 规则卡片之间补 4dp 垂直间距，不再首尾相连糊成一整块

## v1.3.0（重构版）

**架构重构 + 全界面回归 MIUIX**

- 编译底座换成上游 GKD 新模块化架构（包名 `li.gkd.app.*`、Navigation3、Room、Ktor），业务逻辑全部使用新底座实现
- 界面层不采用上游 UI，而是把旧版 MIUIX 渲染层原样「翻译」到新底座：四个 Tab 页 + 二级页面（高级设置、工作模式、关于、AI 服务商、订阅详情、快照等）全部按旧版观感重排
- 应用更名为 **GKD-XA**
- 补回底座缺失的毛玻璃二级页壳 `GkPageScaffold`（等价旧版 `AppPageScaffold`）：顶栏滚动透出毛玻璃、落定转实色
- 二级页返回按钮统一为 MIUIX 无边框图标按钮，消除顶栏风格割裂

**新增 / 增强**

- AI 规则改为多服务商：新增「AI 服务商」列表页，可同时保存多套协议/地址/密钥/模型池，勾选圆点即切换当前服务商
- 服务商详情拆成「配置 / 模型」两个页签；配置页含 Base URL、API Key（可显隐）、端点模式（Chat Completions / Responses）、`anthropic-version`、自定义请求头、系统提示词与生成参数
- 模型页支持远端拉取并按 Model ID 合并、关键字搜索、多选批量删除、逐个编辑上下文长度与思考标记
- 「测试连接」直接读取远端 `/models` 并把返回的模型并入列表，结果在行内即时反馈
- 首次启动自动把旧的单份 AI 配置迁移成一个服务商，无需重填
- 通知文案可自定义主标题 / 副标题 / 正文，支持模板变量与实时预览
- 规则类别：前缀匹配、跟随订阅或规则组默认值、批量设置与冲突检测
- 控制关系图：可视化规则组开关来源（订阅 / 类别 / 应用 / 自身设置）

**修复**

- 修复首页右上角「工作模式」按钮点按闪退：MIUIX 部分 `TextStyle` 的 `lineHeight` / `fontSize` 不是 `Sp` 单位，直接 `toDp()` 会抛 `IllegalStateException: Only Sp can convert to Px`；新增安全工具 `toSpDpOr()` / `lineHeightDp()` 兜底，并全局修掉 3 处同类隐患
- 修复订阅卡片无障碍语义与长按误取消（长按改为 `select` 而非 `toggle`）
- 修复订阅页丢失全选 / 反选入口
- 修复「局部关闭」引导内「继续」按钮点击无响应
- 修复通知文案弹窗在切换开关时改变 composable 调用点数量的问题
- 应用详情页（应用列表点入）：规则卡片、订阅分组标题、排序/筛选菜单与返回按钮恢复旧版 MIUIX 的间距和组件观感；保留新版批量选择、规则可用性说明、控制关系等业务能力
## v1.2.7

- 更新弹窗的确认按钮改为「更新」，四个字不再折成两行
- 下载失败时弹窗新增「复制链接 / 浏览器打开」，GitHub 直连不通可交给带代理的浏览器下载

## v1.2.6

- AI 规则改为多服务商：新增「AI 服务商」列表页，可同时保存多套协议/地址/密钥/模型池，勾选圆点即切换当前服务商
- 服务商详情拆成「配置 / 模型」两个页签；配置页含 Base URL、API Key（可显隐）、端点模式（Chat Completions / Responses）、anthropic-version、自定义请求头、系统提示词与生成参数
- 模型页支持远端拉取并按 Model ID 合并、关键字搜索、多选批量删除、逐个编辑上下文长度与思考标记
- 「测试连接」直接读取远端 /models 并把返回的模型并入列表，结果在行内即时反馈
- 首次启动自动把旧的单份 AI 配置迁移成一个服务商，无需重填

## v1.2.5

- 修复开机后需手动打开「服务状态」：系统杀掉无障碍时不再误清开启意图，自动修复会补写无障碍总开关
- 修复无障碍进入 Crashed 后开关显示开启但跳广告失效、且关不掉：关闭时从安全设置移除，开启/修复时先摘再挂

## v1.2.4

- 修复悬浮底栏液态玻璃边缘折射/反射错位（padding 须在 blur 之前）
- 更新检测优先 GitHub raw，多源比对最高 versionCode，避免 CDN 脏缓存挡住更新

## v1.2.3

- 按 MIUIX 规范收敛界面：二级页统一 AppPageScaffold 毛玻璃顶栏
- 修复首页顶栏模糊不生效；底栏避免多 backdrop 抢采样变黑
- 服务状态语义色、进度指示统一封装；卡片 12.dp 与可点色块 squircle

## v1.2.2

- 测试通知文案说明：应用内不会上岛，请在通知栏查看实时通知

## v1.2.1

- 实时通知同时适配 ColorOS 流体云与 HyperOS 超级岛
- 超级岛参数改为官方摘要态模板（含 miui.focus.pics）

## v1.2.0

- 去掉设置分组外的分类小字标题（如「服务」「数据概览」「快捷入口」等），更贴合 MIUIX 观感

## v1.1.0

- 应用更名为 GKD-X，安装包名改为 `li.songe.gkdx`（可与原版 GKD 并存）
- 触发记录、应用配置（含「最近触发」）改为独立 Activity，缓解进页转场掉帧

## v1.0.0（首版）

- 全量 MIUIX 界面（顶栏模糊、悬浮底栏、液态玻璃 FAB）
- 预测式返回开关；动态取色默认关闭
- 订阅添加/修改/刷新时显示加载进度
- 触发提示支持流体云 / 灵动岛实时通知，并可自定义存在时间

## MIUIX

- 界面全面适配 MIUIX（顶栏、底栏、设置分组、对话框、图标等）
- 触发记录页适配 MIUIX 样式与字体色
- 订阅页取消下拉刷新，顶栏增加刷新按钮

## 开源致谢

本分支基于 [GKD](https://github.com/gkd-kit/gkd) 与 [hanchuan8/gkd-miuix](https://github.com/hanchuan8/gkd-miuix)，界面依托 [compose-miuix-ui](https://github.com/compose-miuix-ui/miuix)（MIUIX UI / Preference / Icons / Blur）等开源项目，详见仓库 README。

## 更新方式

- GKD-X - 设置 - 关于 - 检测更新
- 或前往 [GitHub Releases](https://github.com/84593320z/gkd-/releases) 下载
