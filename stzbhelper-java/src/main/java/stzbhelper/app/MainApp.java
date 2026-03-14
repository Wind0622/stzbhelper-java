package stzbhelper.app;

import stzbhelper.capture.CaptureService;
import stzbhelper.dispatch.CommandDispatcher;
import stzbhelper.global.GlobalState;
import stzbhelper.global.LogUtil;
import stzbhelper.storage.Database;
import stzbhelper.storage.StorageService;
import stzbhelper.web.HttpServer;
import java.io.PrintStream;
import java.io.File;

public class MainApp {
  public static void main(String[] args) {
    setupConsoleEncoding();
    ensureDataDir();
    try {
      Database database = new Database();
      StorageService storage = new StorageService(database);
      storage.init("stzbhelper");

      LogUtil.info("stzbHelper开始运行!");
      LogUtil.info("version: " + GlobalState.version);

      HttpServer httpServer = new HttpServer(storage);
      int port = resolvePort(args);
      httpServer.start(port);
      LogUtil.info("HTTP服务启动");
      LogUtil.info("http://127.0.0.1:" + port + " 浏览器打开此地址控制软件");
      LogUtil.info("等待打开主公簿激活软件...");
      LogUtil.info("未打开主公簿激活软件前软件可能会出现报错！");

      CommandDispatcher dispatcher = new CommandDispatcher(storage);
      CaptureService captureService = new CaptureService(dispatcher);
      captureService.start();
    } catch (Exception e) {
      LogUtil.error("Critical error during startup:");
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static void setupConsoleEncoding() {
    try {
      // 检查当前是否在 Windows 的终端环境下运行
      String os = System.getProperty("os.name").toLowerCase();
      if (os.contains("win")) {
        // 在 Windows 下，尝试强制输出为 UTF-8 以匹配 run.bat 中的 chcp 65001
        // 如果依然乱码，可以考虑切换回 GBK，但目前 run.bat 已经 chcp 65001 了
        System.setOut(new PrintStream(System.out, true, "UTF-8"));
        System.setErr(new PrintStream(System.err, true, "UTF-8"));
      }
    } catch (Exception ignored) {}
  }

  private static void ensureDataDir() {
    File dataDir = new File("data");
    if (!dataDir.exists()) {
      dataDir.mkdirs();
    }
  }

  private static int resolvePort(String[] args) {
    if (args != null && args.length > 0) {
      try {
        int port = Integer.parseInt(args[0]);
        if (port > 0 && port < 65536) {
          return port;
        }
      } catch (NumberFormatException ignored) {
        // ignore
      }
    }
    String envPort = System.getenv("STZB_PORT");
    if (envPort != null && !envPort.isBlank()) {
      try {
        int port = Integer.parseInt(envPort);
        if (port > 0 && port < 65536) {
          return port;
        }
      } catch (NumberFormatException ignored) {
        // ignore
      }
    }
    return 9527;
  }
}
