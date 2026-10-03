# 更新内容

## v1.3.2（2026-10-03）

**界面层整体回退到 v1.2.7 观感**

- 底座与业务能力保持 v1.3.1 不变，界面层整体恢复为 v1.2.7 那套 MIUIX 渲染层（即 v1.3.0 移植到新底座后的版本）：
  - 恢复自研液态玻璃悬浮底栏（`ui/liquid` 整包）与配套设置开关
  - 恢复 `GkTriStateSwitch`、`GkSubsItemCard`、`GkAuthCard`、`GkAuthButtonGroup` 等自绘组件
  - 恢复旧版设置分组 / 卡片 / 对话框 / 输入框样式（撤销 1.3.1 的 GkRowDefaults 排版统一与 M3→miuix 组件替换）
  - 恢复应用配置页「本地订阅」分组入口与订阅分组标题
- 保留 v1.3.1 的全部修复（在恢复后的界面上重新应用）：
  - 首页「应用」数量与「应用」Tab 同源（`AppInfoRepository.visibleAppInfosFlow`）
  - 应用列表点击闪退（TextStyle placeholder 的 lineHeight/width 兜底）
  - 订阅卡片按压高亮露出尖角
  - 二级页顶栏（多选顶栏）上方大片留白（改用 SmallTopAppBar）
- 补齐 1.3.0 翻译版遗漏的 1.2.7 界面元素：
  - 悬浮按钮恢复液态玻璃（vibrancy + 折射 + 高光，无 RuntimeShader 时回退实色），涉及 8 个页面
  - 关于页整页恢复 1.2.7 版式：滚动视差 Logo（渐变收起/标题淡入）、背景特效（BgEffect 组件包整体移植）、
    顶栏滚动模糊落定转实色；功能保持新底座实现（更新渠道/检查更新/反馈/导出日志/分享 APK）
- 恢复「触发提示 → 实时通知（流体云/超级岛）」完整功能（1.3.0 换底座时丢失，表现为选项点击无反应）：
  - 设置项恢复三选一样式（悬浮窗 / 系统 Toast / 实时通知）与选中态正确回显
  - 移植 ActionTipNotif 渲染器：同一条 ongoing 通知同时适配 ColorOS 流体云（Google Live Update，
    反射调用避免 ROM 缺方法崩溃）与 HyperOS 超级岛（miui.focus 模板）；通知权限缺失/无 Live Update
    API 时给出结果提示
  - 恢复「存在时间」快选与自定义秒数（2-120 秒，到期自动取消）、「实时更新系统开关」直达入口、
    「发送测试通知」按当前样式分发并在需要时先请求通知权限
  - store 新增 actionTipStyle / actionTipLiveDurationSec（带默认值，旧配置兼容）；
    resolveActionTipStyle 兼容旧版 useSystemToast 开关
- 修复 AI 服务商三个页面（列表 / 详情 / 使用说明）顶栏与页签底色异常：
  1.3.0 移植时这三个页面用了 material3 Scaffold + GkTopAppBar（顶栏恒为实色 surface、
  页面体为 background 色，与 1.2.7 毛玻璃壳的 surface 容器形成色差）。现统一换回
  GkPageScaffold 毛玻璃二级页壳（顶栏滚动透出毛玻璃、落定转实色），与 1.2.7 一致
- 全 App 二级页壳统一：其余 20 处仍使用 material3 Scaffold 的页面全部换回
  GkPageScaffold 毛玻璃壳，彻底消除顶栏/页面体的 surface–background 色差：
  - 订阅流 6 页（应用列表、应用规则组、分类、分类规则组、全局规则组、全局排除，
    多选顶栏接壳的 externalScrollBehavior + barColor）
  - 日志 3 页（活动记录、触发记录、无障碍事件）
  - 编辑器壳 GkEditorScaffold（覆盖规则组/分类/规则排除/屏蔽列表编辑、
    触发提示文案、通知文案 6 个编辑页）
  - 应用作用域 2 页、快照页、快照设置页、局部关闭引导页、崩溃报告页（新增 bottomBar 槽）、
    内置浏览器（关闭内容采样避免重绘闪烁）、规则源查看与规则控制关系全屏弹窗

## v1.3.1（2026-10-03）

**全 App 统一分组与行排版**

- 新增 `GkRowDefaults` 作为全 App 唯一的排版规格来源：卡片圆角 24、左右外边距 16、分组间距 16、行高 ≥52、
  行内边距 16/12、行首图标 24 + 间距 16、分组小标题 14 Bold（缩进 32）、分割线 0.33dp@10%
- 设置分组现在会真正渲染「常规 / 无障碍 / 外观 / 其他」这类小标题（此前该参数被忽略）
- 设置项与开关项改为统一的「标题 16 Medium + 摘要 14」两行结构，行尾箭头改为细箭头
- 按压反馈统一为「按住整行叠 6% 前景色」（去掉涟漪），与 MIUIX 组件行为一致
- 组内分割线自动生成（miuix 组件本身不画行间分割线）：非首行自动画一条 0.33dp 细线
- 首页 / 订阅 / 应用 / 设置四个主 Tab、应用配置页规则卡、应用列表卡、订阅卡全部统一到该规格

- Material3 组件继续替换为官方 miuix：单选按钮、下拉刷新、Tooltip、复选框、底部操作条、
  普通按钮、对话框（17 处调用点零改动）、Scaffold（22 个文件）、输入框（新增 GkTextField 封装，
  删除从 material3 手工拷贝的 GkOutlinedTextField）

**AI 配置界面重排**

- 服务商列表：顶部圆角搜索框，「添加服务商」入口改为「标题 + 摘要 + 右箭头」行，已配置项改为
  名称 / 地址 / 元信息三段式，行尾圆点即选即切当前服务商，删除由长按触发并走统一弹窗
- 服务商详情（配置）：输入项改为「label 当占位」的紧凑写法，端点模式改为行尾值 + 箭头，
  测试连接为标准行；自定义请求头可展开；启用开关 + 系统提示词 + 生成参数分组；
  底部是全宽主按钮 + 状态文字，危险操作为居中红字卡片
- 模型页签：容器与行样式同步到同一套排版规格
- 排版规格统一：卡片圆角 24、左右外边距 16、分组间距 16、行高 ≥52、行内边距 16/12、
  行首图标 24 + 间距 16、分割线 0.33dp@10%（有图标时左缩进 56）、
  主按钮 44 高/圆角 22、弹窗内外边距 24/16、分组小标题 14 Bold 缩进 32

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
