package stzbhelper.app;

import stzbhelper.capture.CaptureService;
import stzbhelper.dispatch.CommandDispatcher;
import stzbhelper.global.GlobalState;
import stzbhelper.storage.Database;
import stzbhelper.storage.StorageService;
import stzbhelper.web.HttpServer;

public class MainApp {
  public static void main(String[] args) throws Exception {
    Database database = new Database();
    StorageService storage = new StorageService(database);
    storage.init("stzbhelper");

    System.out.println("stzbHelper开始运行");
    System.out.println("version: " + GlobalState.version);

    HttpServer httpServer = new HttpServer(storage);
    int port = resolvePort(args);
    httpServer.start(port);
    System.out.println("HTTP服务已启动: http://127.0.0.1:" + port);

    CommandDispatcher dispatcher = new CommandDispatcher(storage);
    CaptureService captureService = new CaptureService(dispatcher);
    captureService.start();
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
