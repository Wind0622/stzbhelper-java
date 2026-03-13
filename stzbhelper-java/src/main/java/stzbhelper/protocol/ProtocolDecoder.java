package stzbhelper.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;

public class ProtocolDecoder {
  public static boolean hasTcpPsh(Packet packet) {
    TcpPacket tcp = packet.get(TcpPacket.class);
    if (tcp == null) {
      return false;
    }
    return tcp.getHeader().getPsh();
  }

  public static byte[] getTcpPayload(Packet packet) {
    TcpPacket tcp = packet.get(TcpPacket.class);
    if (tcp == null || tcp.getPayload() == null) {
      return null;
    }
    return tcp.getPayload().getRawData();
  }

  public static int readInt32(byte[] buf, int offset) {
    if (buf == null || buf.length < offset + 4) {
      return 0;
    }
    return ByteBuffer.wrap(buf, offset, 4).order(ByteOrder.BIG_ENDIAN).getInt();
  }

  public static byte[] extractPayload(byte[] packet) {
    if (packet.length <= 17) {
      return new byte[0];
    }
    byte[] data = new byte[packet.length - 17];
    System.arraycopy(packet, 17, data, 0, data.length);
    return data;
  }

  public static byte[] decodeData(int dataType, byte[] payload) {
    if (dataType == 3) {
      return ZlibDecoder.decode(payload);
    }
    if (dataType == 5) {
      return XorDecoder.decode(payload);
    }
    return payload;
  }

  public static byte[] parseZlibData(byte[] data) {
    if (data == null || data.length < 2) {
      return new byte[0];
    }
    if ((data[0] & 0xFF) == 120 && (data[1] & 0xFF) == 156) {
      return ZlibDecoder.decode(data);
    }
    return data;
  }
}
