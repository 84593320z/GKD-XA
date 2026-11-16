# GKD-XA v1.5.2 修复与验收范围

- auth.status: granted
- 授权依据：仓库所有者要求修复原审查与复核确认的全部问题，完成独立复核和测试后推送并发布。
- in_scope: [84593320z/GKD-XA](https://github.com/84593320z/GKD-XA)，基线提交 `87602f514934cb5204286489352ede4083211128`；原审查 F01–F22、复核 N01–N05、相关 R/C 条件项与修复期间发现的回归。
- network_profile: repository-maintenance；读取依赖契约、下载构建依赖、通过仓库已有认证推送分支/主线/标签并使用已有 CI 签名发布。
- out_of_scope: 未连接或操作用户 Android 设备，未调用真实 AI 服务商，未读取或公开签名私钥、令牌和用户数据，未更改客户端全局配置。
- 验证分层：源码审查、生产 helper/JVM/SQLite/受控浏览器测试、优化 APK 构建；这些结果不等于 Android/ROM/真实服务商验收。
- 发布身份：`li.songe.gkdx`，versionCode `153`，versionName `1.5.2`，标签 `v1.5.2`。
- 签名约束：沿用上一正式版本证书 SHA-256 `9361daaec8ad21965fb3b658dcfc57ebaebfc0a06df157df6d60e0438e072ea9`，由发布脚本校验。
