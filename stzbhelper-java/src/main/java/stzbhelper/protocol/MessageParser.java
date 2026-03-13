package stzbhelper.protocol;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import stzbhelper.global.GlobalState;
import stzbhelper.model.BattleData;
import stzbhelper.model.BattleReport;
import stzbhelper.model.Report;
import stzbhelper.model.TeamUser;
import stzbhelper.storage.StorageService;

public class MessageParser {
  private final StorageService storage;
  private final ObjectMapper mapper;

  public MessageParser(StorageService storage) {
    this.storage = storage;
    this.mapper = storage.getMapper();
  }

  public void parseData(int cmdId, byte[] data) {
    if (GlobalState.isDebug) {
      byte[] msg = ProtocolDecoder.parseZlibData(data);
      System.out.println("Received [" + cmdId + "] message: " + new String(msg, StandardCharsets.UTF_8));
    }

    if (cmdId == 103) {
      parseTeamUser(data);
    } else if (cmdId == 92) {
      if (GlobalState.exVar.needGetBattleData) {
        System.out.println("Detailed battle report collection enabled, pausing attendance report collection.");
        parseBattleData(data);
      } else {
        parseReport(data);
      }
    }
  }

  private void parseTeamUser(byte[] data) {
    System.out.println("Received team member message");
    byte[] msgData = ProtocolDecoder.parseZlibData(data);
    if (msgData.length == 0) {
      System.out.println("Failed to parse team member message");
      return;
    }
    try {
      List<List<Object>> jsondata = mapper.readValue(msgData, new TypeReference<List<List<Object>>>() {});
      List<TeamUser> users = new ArrayList<>();
      List<Integer> ids = new ArrayList<>();
      for (List<Object> item : jsondata) {
        TeamUser user = TeamUser.fromRawList(item);
        if (user != null) {
          users.add(user);
          ids.add(user.id);
        }
      }
      System.out.println("Team member message parsed successfully, total: " + users.size());
      storage.saveTeamUsers(users);
      storage.deleteTeamUsersNotIn(ids);
    } catch (Exception e) {
      System.out.println("Failed to parse team member message");
    }
  }

  private void parseReport(byte[] data) {
    System.out.println("Received team battle report message");
    if (!GlobalState.exVar.needGetReport) {
      System.out.println("Report collection disabled, skipping parsing.");
      return;
    }
    byte[] msgData = ProtocolDecoder.parseZlibData(data);
    if (msgData.length == 0) {
      System.out.println("Failed to parse team battle report message");
      return;
    }
    try {
      List<List<Object>> jsondata = mapper.readValue(msgData, new TypeReference<List<List<Object>>>() {});
      List<Report> neededReports = new ArrayList<>();
      List<String> rawJson = new ArrayList<>();

      for (List<Object> v : jsondata) {
        if (v.isEmpty()) {
          continue;
        }
        Object mapObj = v.get(0);
        if (!(mapObj instanceof Map)) {
          continue;
        }
        Report report = mapper.convertValue(mapObj, Report.class);
        if (report.wid == GlobalState.exVar.neededReportPos) {
          neededReports.add(report);
          rawJson.add(mapper.writeValueAsString(mapObj));
        }
      }

      System.out.println("Team battle report parsed successfully, total: " + jsondata.size() + ", matching: " + neededReports.size());
      if (!neededReports.isEmpty()) {
        storage.saveReports(neededReports, rawJson);
      }
    } catch (Exception e) {
      System.out.println("Failed to parse team battle report message");
    }
  }

  private void parseBattleData(byte[] data) {
    byte[] msgData = ProtocolDecoder.parseZlibData(data);
    if (msgData.length == 0) {
      return;
    }
    try {
      List<Object> rawData = mapper.readValue(msgData, new TypeReference<List<Object>>() {});
      int battleCount = 0;
      for (Object item : rawData) {
        if (!(item instanceof List)) {
          continue;
        }
        List<?> battleArray = (List<?>) item;
        if (battleArray.isEmpty()) {
          continue;
        }
        Object battleMap = battleArray.get(0);
        if (!(battleMap instanceof Map)) {
          continue;
        }
        BattleData battleData = mapper.convertValue(battleMap, BattleData.class);
        BattleReport report = buildBattleReport(battleData);
        report = parseHeroInfo(report);
        storage.saveBattleReport(report);
        battleCount++;
      }
      System.out.println("Processed " + battleCount + " battle records");
    } catch (Exception e) {
      System.out.println("Failed to parse detailed battle report");
    }
  }

  private BattleReport buildBattleReport(BattleData battleData) {
    BattleReport report = new BattleReport();
    report.battleId = battleData.battleId;
    report.attackHelpId = battleData.attackHelpId;
    report.time = battleData.time;
    report.wid = widToString(battleData.wid);
    report.widName = battleData.widName;
    report.attackName = battleData.attackName;
    report.attackUnionName = battleData.attackUnionName;
    report.attackClanName = battleData.attackClanName;
    report.defendClanName = battleData.defendClanName;
    report.defendName = battleData.defendName;
    report.defendUnionName = battleData.defendUnionName;
    report.attackAdvance = battleData.attackAdvance;
    report.attackAllHeroInfo = battleData.attackAllHeroInfo;
    report.attackerGearInfo = battleData.attackerGearInfo;
    report.defendAdvance = battleData.defendAdvance;
    report.defendAllHeroInfo = battleData.defendAllHeroInfo;
    report.defenderGearInfo = battleData.defenderGearInfo;
    report.attackHeroType = battleData.attackHeroType;
    report.attackHeroTypeAdvance = battleData.attackHeroTypeAdvance;
    report.defendHeroType = battleData.defendHeroType;
    report.defendHeroTypeAdvance = battleData.defendHeroTypeAdvance;
    report.attackHp = battleData.attackHp;
    report.defendHp = battleData.defendHp;
    report.npc = battleData.npc;
    report.allSkillInfo = battleData.allSkillInfo;
    report.result = battleData.result;
    report.attackIdu = battleData.attackIdu;
    report.defendIdu = battleData.defendIdu;
    return report;
  }

  private BattleReport parseHeroInfo(BattleReport report) {
    List<List<String>> attackAdvance = splitAndFilter(report.attackAdvance, ";");
    long attackTotal = 0;
    for (int i = 0; i < attackAdvance.size(); i++) {
      if (i == 0) {
        continue;
      }
      List<String> advance = attackAdvance.get(i);
      if (!advance.isEmpty()) {
        long star = parseLongSafe(advance.get(0));
        switch (i) {
          case 1 -> report.attackHero1Star = star;
          case 2 -> report.attackHero2Star = star;
          case 3 -> report.attackHero3Star = star;
          default -> {}
        }
        attackTotal += star;
      }
    }
    report.attackTotalStar = attackTotal;

    List<List<String>> defendAdvance = splitAndFilter(report.defendAdvance, ";");
    long defendTotal = 0;
    for (int i = 0; i < defendAdvance.size(); i++) {
      if (i == 3) {
        continue;
      }
      List<String> advance = defendAdvance.get(i);
      if (!advance.isEmpty()) {
        long star = parseLongSafe(advance.get(0));
        switch (i) {
          case 0 -> report.defendHero3Star = star;
          case 1 -> report.defendHero2Star = star;
          case 2 -> report.defendHero1Star = star;
          default -> {}
        }
        defendTotal += star;
      }
    }
    report.defendTotalStar = defendTotal;

    List<List<String>> attackHeroInfo = splitAndFilter(report.attackAllHeroInfo, ";");
    for (int i = 0; i < attackHeroInfo.size(); i++) {
      List<String> hero = attackHeroInfo.get(i);
      if (hero.size() >= 2) {
        long heroId = parseLongSafe(hero.get(0));
        long heroLevel = parseLongSafe(hero.get(1));
        switch (i) {
          case 0 -> {
            report.attackHero1Id = heroId;
            report.attackHero1Level = heroLevel;
          }
          case 1 -> {
            report.attackHero2Id = heroId;
            report.attackHero2Level = heroLevel;
          }
          case 2 -> {
            report.attackHero3Id = heroId;
            report.attackHero3Level = heroLevel;
          }
          default -> {}
        }
      }
    }

    List<List<String>> defendHeroInfo = splitAndFilter(report.defendAllHeroInfo, ";");
    for (int i = 0; i < defendHeroInfo.size(); i++) {
      List<String> hero = defendHeroInfo.get(i);
      if (hero.size() >= 2) {
        long heroId = parseLongSafe(hero.get(0));
        long heroLevel = parseLongSafe(hero.get(1));
        switch (i) {
          case 0 -> {
            report.defendHero1Id = heroId;
            report.defendHero1Level = heroLevel;
          }
          case 1 -> {
            report.defendHero2Id = heroId;
            report.defendHero2Level = heroLevel;
          }
          case 2 -> {
            report.defendHero3Id = heroId;
            report.defendHero3Level = heroLevel;
          }
          default -> {}
        }
      }
    }

    return report;
  }

  private List<List<String>> splitAndFilter(String input, String delimiter) {
    if (input == null || input.isEmpty()) {
      return new ArrayList<>();
    }
    String[] parts = input.split(delimiter);
    List<List<String>> result = new ArrayList<>();
    for (String part : parts) {
      if (!part.isEmpty()) {
        String[] subParts = part.split(",");
        List<String> filtered = new ArrayList<>();
        for (String subPart : subParts) {
          if (!subPart.isEmpty()) {
            filtered.add(subPart);
          }
        }
        if (!filtered.isEmpty()) {
          result.add(filtered);
        }
      }
    }
    return result;
  }

  private long parseLongSafe(String value) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private String widToString(Object wid) {
    if (wid == null) {
      return "";
    }
    if (wid instanceof Number) {
      return String.valueOf(((Number) wid).longValue());
    }
    return String.valueOf(wid);
  }
}
