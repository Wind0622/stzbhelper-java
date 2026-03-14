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
      handle.setFilter("tcp and src port 8001", BpfProgram.BpfCompileMode.OPTIMIZE);
      while (true) {
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

        if (GlobalState.exVar.bindIpInfo
            && !GlobalState.onlySrcIp.isEmpty()
            && !GlobalState.onlyDstIp.isEmpty()) {
          if (!GlobalState.onlySrcIp.equals(srcIp) || !GlobalState.onlyDstIp.equals(dstIp)) {
            if (GlobalState.isDebug) {
              System.out.println("IP mismatch, skipping data processing");
            }
            continue;
          }
        }

        dispatcher.dispatch(full, srcIp, dstIp);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
