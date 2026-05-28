继承Project.md
[Project.md](Project.md)

# 技术变更通知：移除 JavaFX 界面，改为本地 Web 控制台架构

## 1. 变更背景
鉴于 JavaFX 在 Windows 环境下的客户端硬件加速（DirectX/OpenGL）及运行时存在严重的兼容性问题（部分系统闪退、白屏），现决定**彻底移除 JavaFX 依赖**。
客户端整体架构调整为 **"无界面后台服务 + 本地浏览器 WebUI"** 模式。

## 2. 新架构设计
客户端将演变为一个纯粹的后台 Daemon（守护进程）程序。

[ 玩家浏览器 (Edge/Chrome) ]
▲
│ (HTTP / WebSocket 访问 http://127.0.0.1:26333)
[ 本地 Java 客户端 (Spring Boot / 内嵌 Web 容器) ] <--- 需管理员权限运行
│
├─► 操作 Wintun 虚拟网卡
│
└─► 通过 UDP 隧道连接远端服务器/好友

### 2.1 前端网页托管方案
为了保证“双击即用”，前端静态资源（HTML/CSS/JS）直接内置在 Java 客户端的 `src/main/resources/static` 目录中。
* 可以使用 Vue/React 编译打包后的静态文件。
* 也可以采用极简的 HTML + Bootstrap + Axios/Fetch，确保体积最小。

### 2.2 本地 API 接口设计（Local Loopback API）
本地 Java 服务的 Web 端口固定为 `26333`（仅绑定 `127.0.0.1`，确保安全）。
并且在网页上实现现在javaFx有的所有功能，不必保留javaFx，直接改为V3版本
---

## 3. CodeX 团队开发调整指南

1. **自动拉起浏览器**：在客户端 Spring Boot 的 `ApplicationRunner` 启动完成后，务必加上 `Desktop.getDesktop().browse()` 逻辑，让用户双击 exe 后，浏览器能自动弹窗打开控制网页，实现“傻瓜化”体验。
2. **打包体积瘦身**：没有了 JavaFX 这个庞然大物，利用 `jlink` 打包出来的 JRE 运行时体积可以缩减一大半，客户端单文件可以控制在 20MB-30MB 左右，非常利于玩家之间分发。
3. **跨域问题（CORS）**：由于整个流程都在 `localhost:26333` 下进行，不存在跨域问题。前端通信极为纯粹。