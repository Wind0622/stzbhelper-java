package stzbhelper.dispatch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import stzbhelper.global.GlobalState;
import stzbhelper.protocol.MessageParser;
import stzbhelper.protocol.ProtocolDecoder;
import stzbhelper.protocol.XorDecoder;
import stzbhelper.storage.StorageService;

public class CommandDispatcher {
  private final StorageService storage;
  private final MessageParser parser;
  private final ObjectMapper mapper;

  public CommandDispatcher(StorageService storage) {
    this.storage = storage;
    this.parser = new MessageParser(storage);
    this.mapper = storage.getMapper();
  }

  public void dispatch(byte[] packet, String srcIp, String dstIp) {
    int bufSize = ProtocolDecoder.readInt32(packet, 0);
    int cmdId = ProtocolDecoder.readInt32(packet, 4);
    int dataType = packet[12] & 0xFF;

    if (cmdId == 92 || cmdId == 103) {
      System.out.println("Captured packet: cmdId=" + cmdId + ", dataType=" + dataType + ", length=" + packet.length);
    }

    if (dataType == 3 && (cmdId == 103 || cmdId == 92)) {
      if (packet.length - bufSize != 4) {
        GlobalState.lossCmdId = cmdId;
        GlobalState.lossBytes = packet;
        GlobalState.packetLoss = true;
        GlobalState.needBufSize = bufSize;
        return;
      }
    } else if (cmdId > 99999 && GlobalState.packetLoss
        && (GlobalState.lossCmdId == 103 || GlobalState.lossCmdId == 92)) {
      byte[] combined = new byte[GlobalState.lossBytes.length + packet.length];
      System.arraycopy(GlobalState.lossBytes, 0, combined, 0, GlobalState.lossBytes.length);
      System.arraycopy(packet, 0, combined, GlobalState.lossBytes.length, packet.length);
      if (combined.length - GlobalState.needBufSize != 4) {
        GlobalState.lossBytes = combined;
      } else {
        GlobalState.packetLoss = false;
        byte[] payload = ProtocolDecoder.extractPayload(combined);
        byte[] data = ProtocolDecoder.decodeData(combined[12] & 0xFF, payload);
        parser.parseData(GlobalState.lossCmdId, data);
      }
      return;
    }

    if (cmdId == 3686 && !GlobalState.databaseSelected) {
      handleDatabaseSelection(packet, dataType, srcIp, dstIp);
    }

    if (dataType != 3) {
      return;
    }

    byte[] payload = ProtocolDecoder.extractPayload(packet);
    byte[] data = ProtocolDecoder.decodeData(dataType, payload);
    parser.parseData(cmdId, data);
  }

  private void handleDatabaseSelection(byte[] packet, int dataType, String srcIp, String dstIp) {
    try {
      byte[] data;
      if (dataType == 5) {
        data = XorDecoder.decode(Arrays.copyOfRange(packet, 12, packet.length));
      } else if (dataType == 3) {
        data = ProtocolDecoder.parseZlibData(ProtocolDecoder.extractPayload(packet));
      } else {
        return;
      }

      List<Object> raw = mapper.readValue(data, new TypeReference<List<Object>>() {});
      if (raw.size() <= 1 || !(raw.get(1) instanceof Map)) {
        return;
      }
      Map<?, ?> dataMap = (Map<?, ?>) raw.get(1);
      Object serverObj = dataMap.get("server");
      Object logObj = dataMap.get("log");
      if (!(serverObj instanceof List) || !(logObj instanceof Map)) {
        return;
      }
      List<?> serverList = (List<?>) serverObj;
      Map<?, ?> logMap = (Map<?, ?>) logObj;
      Object roleNameObj = logMap.get("role_name");
      if (serverList.isEmpty() || roleNameObj == null) {
        return;
      }
      String roleName = String.valueOf(roleNameObj);
      String serverName = String.valueOf(serverList.get(0));
      String dbName = roleName + "_" + serverName;

      System.out.println("Received user profile data, switching to database: " + dbName + ".db");
      storage.switchTo(dbName);
      GlobalState.onlySrcIp = srcIp;
      GlobalState.onlyDstIp = dstIp;
      GlobalState.databaseSelected = true;
    } catch (Exception e) {
      System.out.println("数据库选择解析失败: " + e.getMessage());
      if (GlobalState.isDebug) {
        System.out.println(new String(packet, StandardCharsets.UTF_8));
      }
    }
  }
}
