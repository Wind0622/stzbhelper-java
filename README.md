# stzbHelper Java 版

这是对 Go 后端的 Java 重构版本，已对齐抓包、协议解析、数据落库和 HTTP 接口。前端静态资源通过后端内置 Web 服务提供。

## 目录结构

```
java-test/
  stzbhelper-java/
    src/main/java/stzbhelper/
      app/       启动入口
      capture/   抓包与拼包
      protocol/  协议解析与解码
      dispatch/  指令分发
      storage/   SQLite 持久化
      web/       HTTP 接口
    src/main/resources/web/dist/  前端构建产物
```

## 环境要求

- Windows 需要安装 Npcap，并允许应用抓包
- 抓包通常需要管理员权限运行
- JDK 21 已在本机路径 `C:\Users\23266\.jdks\temurin-21`

## 启动引导

### 1. 环境检查
- **Npcap**: Windows 用户必须安装 [Npcap](https://npcap.com/)，并勾选 "Install Npcap in WinPcap API-compatible Mode"。
- **JDK 21**: 确保本地已安装 JDK 21（推荐使用 Temurin-21）。
- **管理员权限**: 抓包功能需要以管理员权限运行终端或 IDE。

### 2. 编译项目
在 `stzbhelper-java` 目录下运行：
```powershell
./gradlew.bat build -x test
```

### 3. 启动服务
你可以通过以下任一方式启动服务（默认监听端口 `9527`）：

- **标准启动**:
  ```powershell
  ./gradlew.bat run
  ```
- **指定端口启动** (例如端口 9528):
  ```powershell
  ./gradlew.bat run --args="9528"
  ```
- **使用环境变量**:
  ```powershell
  $env:STZB_PORT=9528
  ./gradlew.bat run
  ```

### 4. 访问 Web 界面
启动成功后，在浏览器访问：
[http://127.0.0.1:9527](http://127.0.0.1:9527)
*(如果指定了其他端口，请相应修改 URL)*

> **提示**: 如果在 PowerShell/CMD 中看到中文字符显示为乱码（如 `锟斤拷`），可以在运行前输入 `[Console]::OutputEncoding = [System.Text.Encoding]::UTF8` (仅限 PowerShell) 切换终端编码。

## 功能映射

| 功能 | Go 版本位置 | Java 模块 |
|---|---|---|
| 抓包入口 | main.go | capture/CaptureService |
| 拼包与 PSH 处理 | main.go | capture/PacketAssembler |
| 协议分发 | parse.go | dispatch/CommandDispatcher |
| zlib 解压 | parse.go | protocol/ZlibDecoder |
| xor 解码 | parse.go | protocol/XorDecoder |
| 数据库 | model/* | storage/* |
| HTTP 接口 | http/* | web/HttpServer |

## 验证流程

1. 启动 Java 服务
2. 进入游戏并触发主公簿数据
3. 观察日志输出是否有协议号与解析结果
4. 确认 SQLite 数据是否写入

## 常见问题

- 端口被占用：使用 `--args` 指定其他端口或设置 `STZB_PORT`
- 抓包失败：确认 Npcap 安装、管理员权限、以及目标端口 `8001`
