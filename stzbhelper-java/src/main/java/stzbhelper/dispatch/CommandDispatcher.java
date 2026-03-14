package stzbhelper.dispatch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import stzbhelper.global.GlobalState;
import stzbhelper.global.LogUtil;
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

    if (cmdId == 3686) {
      handleDatabaseSelection(packet, dataType, srcIp, dstIp);
    }

    if (dataType != 3 && dataType != 5) {
      return;
    }

    byte[] data;
    if (dataType == 3) {
      byte[] payload = ProtocolDecoder.extractPayload(packet);
      data = ProtocolDecoder.decodeData(dataType, payload);
    } else {
      byte[] raw = Arrays.copyOfRange(packet, 12, packet.length);
      data = ProtocolDecoder.decodeData(dataType, raw);
    }
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
      int serverPort = extractServerPort(serverList);
      if (serverPort > 0) {
        GlobalState.capturePort = serverPort;
      }

      LogUtil.info("服务器信息: " + formatServerInfo(serverList));
      LogUtil.info("角色名: " + roleName);
      LogUtil.info("本地IP：" + dstIp);
      LogUtil.info("游戏服务器IP：" + srcIp);
      LogUtil.info("收到主公簿数据，将打开数据库文件" + dbName + ".db");
      if (!dbName.equals(GlobalState.currentDbName)) {
        storage.switchTo(dbName);
        GlobalState.currentDbName = dbName;
      }
      GlobalState.onlySrcIp = srcIp;
      GlobalState.onlyDstIp = dstIp;
      if (shouldBindIp()) {
        GlobalState.exVar.bindIpInfo = true;
      }
      GlobalState.databaseSelected = true;
    } catch (Exception e) {
      LogUtil.info("\u6570\u636e\u5e93\u9009\u62e9\u89e3\u6790\u5931\u8d25: " + e.getMessage());
      if (GlobalState.isDebug) {
        System.out.println(new String(packet, StandardCharsets.UTF_8));
      }
    }
  }

  private String formatServerInfo(List<?> serverList) {
    if (serverList == null || serverList.isEmpty()) {
      return "[]";
    }
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    for (int i = 0; i < serverList.size(); i++) {
      if (i > 0) {
        sb.append(" ");
      }
      sb.append(String.valueOf(serverList.get(i)));
    }
    sb.append("]");
    return sb.toString();
  }

  private boolean shouldBindIp() {
    String env = System.getenv("STZB_BIND_IP");
    if (env != null && !env.isBlank()) {
      return "1".equals(env) || "true".equalsIgnoreCase(env);
    }
    String prop = System.getProperty("stzb.bind.ip");
    if (prop != null && !prop.isBlank()) {
      return "1".equals(prop) || "true".equalsIgnoreCase(prop);
    }
    return false;
  }

  private int extractServerPort(List<?> serverList) {
    if (serverList == null || serverList.isEmpty()) {
      return 0;
    }
    int candidate = 0;
    for (Object item : serverList) {
      if (item instanceof Number) {
        int v = ((Number) item).intValue();
        if (v >= 1024 && v <= 65535) {
          if (v > candidate) {
            candidate = v;
          }
        }
      } else if (item != null) {
        try {
          int v = Integer.parseInt(String.valueOf(item));
          if (v >= 1024 && v <= 65535) {
            if (v > candidate) {
              candidate = v;
            }
          }
        } catch (NumberFormatException ignored) {
          // ignore
        }
      }
    }
    return candidate;
  }
}
