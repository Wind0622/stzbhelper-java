package stzbhelper.app;

import stzbhelper.capture.CaptureService;
import stzbhelper.dispatch.CommandDispatcher;
import stzbhelper.global.GlobalState;
import stzbhelper.storage.Database;
import stzbhelper.storage.StorageService;
import stzbhelper.web.HttpServer;
import java.io.PrintStream;

public class MainApp {
  public static void main(String[] args) {
    try {
      Database database = new Database();
      StorageService storage = new StorageService(database);
      storage.init("stzbhelper");

      System.out.println("stzbHelper 正在运行...");
      System.out.println("版本: " + GlobalState.version);

      HttpServer httpServer = new HttpServer(storage);
      int port = resolvePort(args);
      httpServer.start(port);
      System.out.println("HTTP 服务已在端口 " + port + " 启动: http://127.0.0.1:" + port);

      CommandDispatcher dispatcher = new CommandDispatcher(storage);
      CaptureService captureService = new CaptureService(dispatcher);
      captureService.start();
    } catch (Exception e) {
      System.err.println("程序启动失败，发生严重错误:");
      e.printStackTrace();
      System.exit(1);
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
