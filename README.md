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

- **Npcap**: Windows 环境下必须安装 Npcap，并勾选 WinPcap 兼容模式。
- **权限**: 抓包模块初始化需要管理员权限运行。
- **JDK**: 本项目基于 JDK 21 运行。

## 启动引导

### 1. 环境准备 (重要)
- **Npcap**: 确保已正确安装 [Npcap](https://npcap.com/)。
- **JDK 21**: 请确保系统已配置好 JDK 21 运行环境。
- **管理员权限**: 请以管理员权限启动终端运行程序。

### 2. 一键启动 (推荐)
在项目根目录下直接运行 `run.bat`：
```powershell
./run.bat
```
*脚本将自动处理编码设置、项目构建及服务启动。*

### 3. 访问界面
启动成功后，浏览器访问：[http://127.0.0.1:9527](http://127.0.0.1:9527)

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
