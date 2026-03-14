package stzbhelper.capture;

import java.util.List;
import org.pcap4j.core.BpfProgram;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV6Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import stzbhelper.global.GlobalState;
import stzbhelper.dispatch.CommandDispatcher;
import stzbhelper.protocol.ProtocolDecoder;

public class CaptureService {
  private final CommandDispatcher dispatcher;
  private final PacketAssembler assembler = new PacketAssembler();
  private final java.util.concurrent.ExecutorService dispatcherExecutor =
      java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "packet-dispatcher");
        t.setDaemon(true);
        return t;
      });

  public CaptureService(CommandDispatcher dispatcher) {
    this.dispatcher = dispatcher;
  }

  public void start() throws Exception {
    List<PcapNetworkInterface> devices = Pcaps.findAllDevs();
    if (devices == null || devices.isEmpty()) {
      throw new IllegalStateException("No network interface found");
    }
    for (PcapNetworkInterface device : devices) {
      Thread thread = new Thread(() -> captureTCPPackets(device));
      thread.setDaemon(true);
      thread.start();
    }
  }

  private void captureTCPPackets(PcapNetworkInterface device) {
    try {
      PcapHandle handle = device.openLive(65535, PromiscuousMode.PROMISCUOUS, 10);
      int lastPort = GlobalState.capturePort;
      handle.setFilter(buildFilter(lastPort), BpfProgram.BpfCompileMode.OPTIMIZE);
      while (true) {
        int currentPort = GlobalState.capturePort;
        if (currentPort != lastPort && currentPort > 0) {
          handle.setFilter(buildFilter(currentPort), BpfProgram.BpfCompileMode.OPTIMIZE);
          lastPort = currentPort;
        }
        Packet packet = handle.getNextPacket();
        if (packet == null) {
          continue;
        }
        byte[] payload = ProtocolDecoder.getTcpPayload(packet);
        if (payload == null || payload.length < 8) {
          continue;
        }
        boolean psh = ProtocolDecoder.hasTcpPsh(packet);
        byte[] full = assembler.acceptPayload(payload, psh);
        if (full == null || full.length < 17) {
          continue;
        }
        TcpPacket tcp = packet.get(TcpPacket.class);
        String srcIp = "";
        String dstIp = "";
        if (tcp != null) {
          String srcPort = String.valueOf(tcp.getHeader().getSrcPort().valueAsInt());
          String dstPort = String.valueOf(tcp.getHeader().getDstPort().valueAsInt());
          IpV4Packet ipv4 = packet.get(IpV4Packet.class);
          IpV6Packet ipv6 = packet.get(IpV6Packet.class);
          if (ipv4 != null) {
            srcIp = ipv4.getHeader().getSrcAddr().getHostAddress() + ":" + srcPort;
            dstIp = ipv4.getHeader().getDstAddr().getHostAddress() + ":" + dstPort;
          } else if (ipv6 != null) {
            srcIp = ipv6.getHeader().getSrcAddr().getHostAddress() + ":" + srcPort;
            dstIp = ipv6.getHeader().getDstAddr().getHostAddress() + ":" + dstPort;
          }
        }

        int cmdId = ProtocolDecoder.readInt32(full, 4);
        if (GlobalState.exVar.bindIpInfo
            && !GlobalState.onlySrcIp.isEmpty()
            && !GlobalState.onlyDstIp.isEmpty()) {
          if (!GlobalState.onlySrcIp.equals(srcIp) || !GlobalState.onlyDstIp.equals(dstIp)) {
            if (cmdId != 3686) {
              if (GlobalState.isDebug) {
                System.out.println("IP mismatch, skipping data processing");
              }
              continue;
            }
          }
        }

        final byte[] packetCopy = full;
        final String srcCopy = srcIp;
        final String dstCopy = dstIp;
        dispatcherExecutor.execute(() -> {
          try {
            dispatcher.dispatch(packetCopy, srcCopy, dstCopy);
          } catch (Exception ignored) {
            // ignore dispatch failures to keep capture running
          }
        });
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private String buildFilter(int port) {
    if (port <= 0 || port == 8001) {
      return "tcp and (src port 8001 or dst port 8001)";
    }
    return "tcp and (src port 8001 or dst port 8001 or src port "
        + port + " or dst port " + port + ")";
  }
}
