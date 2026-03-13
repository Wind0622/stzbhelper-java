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

## 启动步骤

在 `java-test/stzbhelper-java` 目录下执行：

```bat
gradlew.bat build -x test
```

启动服务（默认端口 9527）：

```bat
gradlew.bat run
```

指定端口启动（示例 9528）：

```bat
gradlew.bat run --args="9528"
```

或使用环境变量：

```bat
set STZB_PORT=9528
gradlew.bat run
```

启动后访问：

```
http://127.0.0.1:9527
```

## 一键启动

在 `java-test` 目录下执行：

```bat
start.bat
```

指定端口示例：

```bat
start.bat 9528
```

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
