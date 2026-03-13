package stzbhelper.protocol;

import java.io.ByteArrayOutputStream;
import java.util.zip.Inflater;

public class ZlibDecoder {
  public static byte[] decode(byte[] data) {
    if (data == null || data.length == 0) {
      return data;
    }
    Inflater inflater = new Inflater();
    inflater.setInput(data);
    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[1024];
      while (!inflater.finished()) {
        int count = inflater.inflate(buffer);
        if (count == 0 && inflater.needsInput()) {
          break;
        }
        output.write(buffer, 0, count);
      }
      return output.toByteArray();
    } catch (Exception e) {
      return data;
    } finally {
      inflater.end();
    }
  }
}
