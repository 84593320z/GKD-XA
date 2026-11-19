# Miuix 统一界面

固定使用 Miuix `0.9.4`。导航仍由 `miuix-nav` 管理，保留全部 34 个目的地及原有服务、订阅、数据库和权限业务流程。

## 单一视觉与组件入口

- 页面使用 `surface`，卡片使用 `surfaceContainer`，弹窗使用原生 `DialogDefaults`；浅色、深色、跟随系统和动态配色共享同一套色板。
- `GkUi` 集中管理 16dp 页面边距、12dp 分区节奏、48dp 最小交互区域和 24dp 标准图标。圆角使用 Miuix 的连续曲线，而非另一套 Material 圆角。
- 历史 `PerfSwitch` / `GkSwitch`、`TextSwitch` / `GkTextSwitch`、`SettingItem` / `GkSettingItem` 等入口现在复用同一个渲染实现，避免不同页面各自演变。禁用颜色、状态说明和自定义行点击保持一致。
- 菜单、下拉刷新、工具提示、加载、输入和底部弹层使用 Miuix。需要独立控制返回/外部点击策略的确认窗保留 Compose window host，内容采用原生 Miuix 几何、字体和按钮。小屏底部显示、宽屏居中；长内容可滚动，操作区保留。
- 常用操作及导航图标采用 Miuix Regular/Basic 图标族；缺少对应符号的少量领域图标保留本来含义。搜索与关闭切换使用原生图标淡变，不再复刻 Material 轮廓。
- 日志导出范围改为单选列表，不会在窄屏挤出；复制按钮与编辑器计数有独立空间；长说明、错误信息和 AI 表单处理换行、大字号和键盘。
- 保留截图/图片查看器的黑底白字，以及代码高亮的语义色：这些内容属于工作区，不套用设置卡片背景。

## 授权页不再是例外

`gkd-priv-ui` 是固定在 `priv-ui:0.20.0` 的 Android 源码级 UI 分支，使用 Miuix 控件，而不是给 Material 授权页换配色。保留原权限/生命周期协调和后端，完整保留 Root、无线 ADB、TCP、外部服务、手动命令、配对、取消、确认、错误和日志流程。

上游来源、许可证、修改清单与冻结文件哈希见 `gkd-priv-ui/README.md`。`verify_fork.py` 检查未修改的后端文件及权限宿主；系统权限弹窗及外部应用画面仍由 Android/外部应用渲染，不强行接管。

## 可重复验证

需要 JDK 21、Android SDK 37、Python 3。CI 包含以下检查并归档离屏 PNG：

```sh
python3 scripts/check_miuix_ui.py
python3 gkd-priv-ui/tools/verify_fork.py
python3 -B -m unittest discover -s scripts -p 'test_*.py'
bash ./gradlew :gkd-priv-ui:testDebugUnitTest :gkd-app:testGkdDebugUnitTest :gkd-selector:jvmTest :gkd-db:jvmTest
bash ./gradlew :gkd-app:assembleGkdDebug :gkd-app:assembleGkdRelease
```

离屏交互/布局回归位于 `gkd-app/src/test/kotlin/li/gkd/app/ui/miuix/`。Native graphics 截图生成到 `gkd-app/build/reports/miuix-ui/`，使用模拟内容，不读取手机数据，也不申请授权。它们不等同于全部页面的真机截图。

## 真机验收边界

源码审查覆盖全部路由及共享组件，但不能据此宣称全部设备、全部页面像素级一致。仍需在解锁的测试设备上检查：

1. 四个首页标签、搜索、下拉刷新、双击回顶、预测式返回，以及悬浮/停靠底栏。
2. 规则/订阅/快照弹层快速开关、旋转、真实加载与失败、键盘下的保存取消。
3. 屏幕裁切、图片预览、缩放和手势坐标；三键导航和刘海/侧边 cutout。
4. 真实权限授予、拒绝、取消、返回、进程重建及外部授权器。验证这些需要人工准备，不自动启动权限服务。

具体复盘发现、修复与最终验证结果见 `miuix-ui-review.md`。
