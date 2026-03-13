package stzbhelper.global;

public final class GlobalState {
  private GlobalState() {}

  public static final WebExVar exVar = new WebExVar();
  public static volatile boolean isDebug = false;
  public static final String version = "0.0.3-rc2-fix2";

  public static volatile String onlySrcIp = "";
  public static volatile String onlyDstIp = "";

  public static volatile boolean packetLoss = false;
  public static volatile byte[] lossBytes = new byte[0];
  public static volatile int lossCmdId = 0;
  public static volatile int needBufSize = 0;

  public static volatile boolean databaseSelected = false;
}
