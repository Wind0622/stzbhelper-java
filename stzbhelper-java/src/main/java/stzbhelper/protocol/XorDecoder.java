package stzbhelper.protocol;

public class XorDecoder {
  public static byte[] decode(byte[] data) {
    if (data == null || data.length == 0) {
      return data;
    }
    if (data[0] != 5) {
      return new byte[0];
    }
    int start = 1;
    byte[] result = new byte[data.length - start];
    for (int i = start; i < data.length; i++) {
      result[i - start] = (byte) (data[i] ^ 152);
    }
    return result;
  }
}
