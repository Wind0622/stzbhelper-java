package stzbhelper.dispatch;

public interface CommandHandler {
  void handle(int cmdId, byte[] data);
}
