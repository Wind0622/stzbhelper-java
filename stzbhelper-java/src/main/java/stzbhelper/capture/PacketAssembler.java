package stzbhelper.capture;

import java.io.ByteArrayOutputStream;

public class PacketAssembler {
  private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
  private boolean waiting = false;

  public synchronized byte[] acceptPayload(byte[] payload, boolean psh) {
    if (!psh) {
      waiting = true;
      buffer.writeBytes(payload);
      return null;
    }
    if (waiting) {
      waiting = false;
      buffer.writeBytes(payload);
      byte[] data = buffer.toByteArray();
      buffer.reset();
      return data;
    }
    return payload;
  }
}
