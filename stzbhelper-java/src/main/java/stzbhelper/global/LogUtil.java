package stzbhelper.global;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class LogUtil {
  private LogUtil() {}

  private static final ThreadLocal<SimpleDateFormat> FORMAT =
      ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"));

  public static void info(String message) {
    System.out.println(formatPrefix() + message);
  }

  public static void error(String message) {
    System.err.println(formatPrefix() + message);
  }

  private static String formatPrefix() {
    return FORMAT.get().format(new Date()) + " ";
  }
}
