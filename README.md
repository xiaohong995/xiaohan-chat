# 小韩 App（XiaoHan）

单页聊天安卓应用，内置人格与关系记忆，通过 DeepSeek API 对话。

## 使用

1. 构建产物 `xiaohan.apk` 在仓库根目录，浏览器直接下载安装
2. 打开 App → 点发送 → 填入 DeepSeek API Key（platform.deepseek.com 开放平台申请）→ 保存
3. 改人格：`app/src/main/java/com/xiaohan/chat/MainActivity.kt` 的 `systemPrompt`，push 自动重新构建
