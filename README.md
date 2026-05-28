# MC-FlashLink V3

MC-FlashLink V3 是面向 Minecraft 玩家的虚拟局域网联机工具。当前方案使用 Wintun/TUN 虚拟网卡承载三层 IP 包，服务端负责房间管理、虚拟网段和虚拟 IP 分配，客户端负责启动本机虚拟网卡并通过 UDP 隧道接入同一个房间网络。

客户端已按 `ProjectV3.md` 改为“无界面后台服务 + 本地浏览器 WebUI”架构，不再依赖 JavaFX。启动客户端后会在本机启动 Spring Boot 服务，并自动打开 `http://127.0.0.1:26333` 控制台。

## 模块

- `common`：共享 DTO 与 UDP 隧道包格式。
- `server`：Spring Boot 服务端，负责房间、心跳、虚拟网段、虚拟 IP 分配和 UDP Relay。
- `client`：Spring Boot 本地客户端服务，内置静态 Web 控制台、Wintun JNA 绑定和 UDP 隧道引擎。

## 环境要求

当前工程按 Java 8 构建：

```powershell
java -version
mvn -version
```

Wintun 虚拟网卡安装、卸载和 IP 配置需要管理员权限。实际联机测试客户端请使用管理员脚本启动。

## 编译

```powershell
mvn compile
```

## 运行服务端

```powershell
mvn -pl server -am spring-boot:run
```

或使用脚本：

```powershell
.\scripts\run-server.ps1
```

默认 HTTP API 端口：`8080`

默认 UDP 隧道中转端口：`21000`

## 打包服务端

```powershell
.\scripts\package-server.ps1
```

或手动在项目根目录执行：

```powershell
mvn -pl server -am clean package -DskipTests
```

不要在 `server` 目录里单独运行 `mvn package`，也不要少写 `-am`，否则 Maven 会找不到同仓库的 `common` 模块。

## 运行客户端

开发模式：

```powershell
mvn -pl client -am compile exec:java
```

客户端启动后访问：

```text
http://127.0.0.1:26333
```

需要测试 Wintun 虚拟网卡时，请用管理员脚本启动客户端，它会触发 UAC：

```powershell
.\scripts\run-client-admin.ps1
```

如果 PowerShell 执行策略拦截脚本，可以直接运行：

```powershell
.\scripts\run-client-admin.cmd
```

## 打包客户端

推荐使用 portable 包，运行目录内会包含 Java runtime，不依赖目标电脑本机安装的 Java：

```powershell
.\scripts\package-client.ps1
```

产物位置：

- `client\target\client-0.1.0-SNAPSHOT.jar`
- `client\target\dist`
- `client\target\client-portable.zip`

解压 `client-portable.zip` 后运行 `run-client-admin.cmd`。脚本会使用相对路径下的 `runtime\bin\java.exe` 启动客户端，并打开本地 Web 控制台。

打包脚本会从指定 JDK/JRE 复制 Java 8 runtime，并清理 JavaFX 运行库文件；客户端本身不再依赖 JavaFX。

如果要指定被打进 portable 包的 JDK/JRE 路径：

```powershell
.\scripts\package-client.ps1 -BundledJdkHome "C:\Program Files\Java\jdk1.8.0_202"
```

如果只想生成不内置 runtime 的轻量包：

```powershell
.\scripts\package-client.ps1 -NoBundledJdk
```

手动执行 Maven 时请在项目根目录运行：

```powershell
mvn -pl client -am clean package -DskipTests
```

不要在 `client` 目录里单独运行 `mvn package`，否则 Maven 找不到同仓库的 `common` 模块。

## 本地客户端接口

这些接口只绑定 `127.0.0.1:26333`，由内置 WebUI 调用：

- `GET /api/local/status`：获取客户端连接状态、房间人数、房主 IP、虚拟网卡状态。
- `POST /api/local/create-room`：房主创建房间并启动 Wintun 隧道。
- `POST /api/local/join-room`：访客加入房间并启动 Wintun 隧道。
- `POST /api/local/leave-room`：访客退出房间并卸载本机虚拟网卡。
- `POST /api/local/dismiss-room`：房主解散房间并卸载本机虚拟网卡。

## 服务端接口

- `POST /api/room/create`：创建房间，分配房主虚拟 IP，例如 `10.26.x.1`。
- `POST /api/room/join`：加入房间，分配访客虚拟 IP，例如 `10.26.x.2`。
- `POST /api/room/info`：查询房间状态和房间人数。
- `POST /api/room/leave`：访客退出房间，释放成员和 UDP Relay 端点。
- `POST /api/room/heartbeat`：房间保活。
- `POST /api/room/dismiss`：房主解散房间，释放房间和虚拟网段。

## 当前状态

已完成：

- 房间管理、成员计数、虚拟网段分配、虚拟 IP 分配。
- 自研 UDP 隧道包格式。
- 服务端 UDP Relay：按 `roomCode + virtualIp` 注册客户端端点，并按目标虚拟 IP 转发 DATA 包。
- 客户端本地 Spring Boot 控制服务，固定绑定 `127.0.0.1:26333`。
- 客户端内置 Web 控制台，不再依赖 JavaFX。
- 客户端启动完成后自动打开本地浏览器控制台。
- 客户端 Wintun JNA 绑定和官方 `wintun.dll` 内置释放。
- 客户端创建/加入房间后启动 Wintun 与 UDP 隧道。
- 客户端创建/加入房间后定时发送心跳，并每 10 秒轮询房间人数和状态。
- 客户端支持访客退出房间、房主解散房间，并卸载本机虚拟网卡。

尚未实现：

- 防火墙规则自动配置。
- P2P 打洞。
- 完整 Wintun 读等待事件优化与生产级资源回收。
