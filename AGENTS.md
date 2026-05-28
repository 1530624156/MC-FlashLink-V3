# MC-FlashLink 协作说明

## 项目背景

MC-FlashLink V3 是面向 Minecraft 玩家的一键虚拟局域网联机工具。当前需求基线是 `ProjectV3.md`：客户端彻底移除 JavaFX，改为“无界面后台服务 + 本地浏览器 WebUI”架构。

核心目标是：

`Minecraft -> Wintun 虚拟网卡 -> Java 客户端 -> UDP 隧道 -> 对端 Java 客户端 -> 对端 Wintun 虚拟网卡 -> Minecraft`

## 技术方向

- 服务端：Spring Boot 2.7.x，负责房间生命周期、虚拟 IP 分配、心跳、后续 STUN/P2P 协商。
- 客户端：Spring Boot 本地服务，固定绑定 `127.0.0.1:26333`，内置 `src/main/resources/static` Web 控制台；通过 JNA 调用 Wintun，创建虚拟网卡并启动隧道引擎。
- 数据通道：使用 UDP 传输封装后的三层 IP 包；不要再实现 Minecraft 专用 TCP 端口转发。
- 初期状态可以使用内存结构，例如 `ConcurrentHashMap`；涉及多实例部署时再引入 Redis。

## 模块边界

- `server`：控制器、IPAM 与 UDP Relay，负责房间、成员、虚拟网段、虚拟 IP、心跳和中转隧道包。
- `client`：本地 Web 控制服务、静态 WebUI、Wintun JNA 绑定与 UDP 隧道引擎。
- `common`：共享 DTO、隧道协议、协议常量、错误码。

## 接口约定

- `POST /api/room/create`：创建房间，返回房间号、房主虚拟 IP、虚拟网段、房间人数。
- `POST /api/room/join`：加入房间，返回访客虚拟 IP、房主虚拟 IP、虚拟网段、房间人数。
- `POST /api/room/heartbeat`：心跳保活，房主连续超时后销毁房间。
- `POST /api/room/dismiss`：房主主动解散房间，释放房间和虚拟网段。
- UDP Relay 默认监听 `21000`，客户端通过 `TunnelPacket` 注册 `roomCode + virtualIp` 后，DATA 包按目标虚拟 IP 转发。

接口实现要明确：

- 房间号生成必须避免冲突。
- 每个房间分配独立虚拟网段，例如 `10.26.x.0/24`。
- 房主默认分配 `10.26.x.1`，访客从 `10.26.x.2` 起递增分配。
- 不要在客户端要求用户输入 Minecraft 端口；V3 目标是通过虚拟局域网自动发现。
- 心跳与连接断开都要触发资源清理，不能只依赖客户端主动退出。

## 虚拟网卡注意事项

- 操作 Wintun、配置 IP、修改路由和防火墙规则需要管理员权限。
- Wintun 绑定位于客户端 `service.wintun` 包，UDP 隧道位于客户端 `service.tunnel` 包。
- 当前客户端已内置 `wintun.dll`，运行时释放到本地缓存后加载；仍需要管理员权限。
- 已实现服务器中转模式，P2P 打洞尚未实现。
- MTU 建议预留为 `1420` 或 `1380`。

## 开发规范

- 默认使用 UTF-8 编码。
- 新增 Java 代码优先使用清晰包结构，例如 `cn.com.mcflashlink.server`、`cn.com.mcflashlink.client`、`cn.com.mcflashlink.common`。
- 修改需求相关行为时，同步更新 `Project.md` 或新增设计文档。
- 中文文档统一使用“房主端 Host”“访客端 Guest”“中心服务端 Server”“虚拟网卡 TUN/Wintun”。

## 验证建议

常规验证：

```powershell
mvn test
mvn compile
```

V3 阶段性验证：

- 客户端启动后监听 `127.0.0.1:26333`，并自动打开本地 Web 控制台。
- 创建房间成功，房主获得 `10.26.x.1`。
- 多个访客加入同一房间，依次获得 `10.26.x.2`、`10.26.x.3`。
- 加入不存在或已过期房间失败。
- 心跳超时后房间被销毁，对应虚拟网段可再次分配。
- 两端启动 Wintun 隧道后，能通过虚拟 IP `ping` 通。

## 当前注意事项

- 当前 `server` 已实现控制流和虚拟 IPAM。
- 当前 `server` 已实现 UDP Relay 中转。
- 当前 `client` 已实现 Wintun JNA 绑定骨架和 UDP TunnelEngine。
- 客户端管理员启动脚本已存在：`scripts/run-client-admin.ps1`。
- 安装包 manifest、防火墙规则和 P2P 打洞尚未实现。
