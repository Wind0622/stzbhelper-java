package stzbhelper.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import stzbhelper.model.BattleReport;
import stzbhelper.model.Report;
import stzbhelper.model.Task;
import stzbhelper.model.TaskUserList;
import stzbhelper.model.TeamUser;

public class StorageService {
  private final Database database;
  private final ObjectMapper mapper;

  public StorageService(Database database) {
    this.database = database;
    this.mapper = new ObjectMapper()
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  public ObjectMapper getMapper() {
    return mapper;
  }

  public synchronized void init(String dbFile) throws SQLException {
    database.init(dbFile);
  }

  public synchronized void switchTo(String dbFile) throws SQLException {
    database.switchTo(dbFile);
  }

  public synchronized void saveTeamUsers(List<TeamUser> users) throws SQLException {
    if (users == null || users.isEmpty()) {
      return;
    }
    String sql = "INSERT OR REPLACE INTO team_user "
        + "(id, name, contribute_total, contribute_week, pos, power, wu, \"group\", join_time) "
        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    Connection conn = database.getConnection();
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      for (TeamUser user : users) {
        stmt.setInt(1, user.id);
        stmt.setString(2, user.name);
        stmt.setInt(3, user.contributeTotal);
        stmt.setInt(4, user.contributeWeek);
        stmt.setInt(5, user.pos);
        stmt.setInt(6, user.power);
        stmt.setInt(7, user.wu);
        stmt.setString(8, user.group);
        stmt.setInt(9, user.joinTime);
        stmt.addBatch();
      }
      stmt.executeBatch();
      System.out.println("Saved/Updated " + users.size() + " team members in database");
    }
  }

  public synchronized void deleteTeamUsersNotIn(List<Integer> ids) throws SQLException {
    Connection conn = database.getConnection();
    if (ids == null || ids.isEmpty()) {
      try (Statement stmt = conn.createStatement()) {
        int deleted = stmt.executeUpdate("DELETE FROM team_user");
        if (deleted > 0) {
          System.out.println("Cleared " + deleted + " old team members from database");
        }
      }
      return;
    }
    StringBuilder sql = new StringBuilder("DELETE FROM team_user WHERE id NOT IN (");
    for (int i = 0; i < ids.size(); i++) {
      if (i > 0) {
        sql.append(",");
      }
      sql.append("?");
    }
    sql.append(")");
    try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
      for (int i = 0; i < ids.size(); i++) {
        stmt.setInt(i + 1, ids.get(i));
      }
      int deleted = stmt.executeUpdate();
      if (deleted > 0) {
        System.out.println("Removed " + deleted + " inactive team members from database");
      }
    }
  }

  public synchronized List<TeamUser> findTeamUsers(String group) throws SQLException {
    Connection conn = database.getConnection();
    List<TeamUser> users = new ArrayList<>();
    String sql = "SELECT id, name, contribute_total, contribute_week, pos, power, wu, "
        + "\"group\", join_time FROM team_user";
    if (group != null && !group.isBlank()) {
      sql += " WHERE \"group\" = ?";
    }
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      if (group != null && !group.isBlank()) {
        stmt.setString(1, group);
      }
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          TeamUser user = new TeamUser();
          user.id = rs.getInt("id");
          user.name = rs.getString("name");
          user.contributeTotal = rs.getInt("contribute_total");
          user.contributeWeek = rs.getInt("contribute_week");
          user.pos = rs.getInt("pos");
          user.power = rs.getInt("power");
          user.wu = rs.getInt("wu");
          user.group = rs.getString("group");
          user.joinTime = rs.getInt("join_time");
          users.add(user);
        }
      }
    }
    return users;
  }

  public synchronized List<TeamUser> findTeamUsersByGroups(List<String> groups) throws SQLException {
    if (groups == null || groups.isEmpty()) {
      return new ArrayList<>();
    }
    Connection conn = database.getConnection();
    StringBuilder sql = new StringBuilder(
        "SELECT id, name, contribute_total, contribute_week, pos, power, wu, "
            + "\"group\", join_time FROM team_user WHERE \"group\" IN (");
    for (int i = 0; i < groups.size(); i++) {
      if (i > 0) {
        sql.append(",");
      }
      sql.append("?");
    }
    sql.append(")");
    List<TeamUser> users = new ArrayList<>();
    try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
      for (int i = 0; i < groups.size(); i++) {
        stmt.setString(i + 1, groups.get(i));
      }
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          TeamUser user = new TeamUser();
          user.id = rs.getInt("id");
          user.name = rs.getString("name");
          user.contributeTotal = rs.getInt("contribute_total");
          user.contributeWeek = rs.getInt("contribute_week");
          user.pos = rs.getInt("pos");
          user.power = rs.getInt("power");
          user.wu = rs.getInt("wu");
          user.group = rs.getString("group");
          user.joinTime = rs.getInt("join_time");
          users.add(user);
        }
      }
    }
    return users;
  }

  public synchronized List<String> findTeamGroups() throws SQLException {
    Connection conn = database.getConnection();
    List<String> groups = new ArrayList<>();
    String sql = "SELECT DISTINCT \"group\" FROM team_user";
    try (PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        groups.add(rs.getString(1));
      }
    }
    return groups;
  }

  public synchronized int createTask(Task task) throws SQLException, JsonProcessingException {
    Connection conn = database.getConnection();
    String sql = "INSERT INTO task "
        + "(status, name, time, pos, target, target_user_num, complete_user_num, user_list) "
        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setInt(1, task.status);
      stmt.setString(2, task.name);
      stmt.setInt(3, task.time);
      stmt.setInt(4, task.pos);
      stmt.setString(5, toJson(task.target));
      stmt.setInt(6, task.targetUserNum);
      stmt.setInt(7, task.completeUserNum);
      stmt.setString(8, toJson(task.userList));
      stmt.executeUpdate();
      try (ResultSet rs = stmt.getGeneratedKeys()) {
        if (rs.next()) {
          return rs.getInt(1);
        }
      }
    }
    return 0;
  }

  public synchronized List<Task> getTaskList(boolean omitUserList) throws SQLException {
    Connection conn = database.getConnection();
    List<Task> tasks = new ArrayList<>();
    String sql = "SELECT id, status, name, time, pos, target, target_user_num, "
        + "complete_user_num, user_list FROM task ORDER BY id DESC";
    try (PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        Task task = readTask(rs);
        if (omitUserList) {
          task.userList = null;
        }
        tasks.add(task);
      }
    }
    return tasks;
  }

  public synchronized Task getTaskById(int id) throws SQLException {
    Connection conn = database.getConnection();
    String sql = "SELECT id, status, name, time, pos, target, target_user_num, "
        + "complete_user_num, user_list FROM task WHERE id = ?";
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, id);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return readTask(rs);
        }
      }
    }
    return null;
  }

  public synchronized int deleteTask(int id) throws SQLException {
    Connection conn = database.getConnection();
    try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM task WHERE id = ?")) {
      stmt.setInt(1, id);
      return stmt.executeUpdate();
    }
  }

  public synchronized int updateTask(Task task) throws SQLException, JsonProcessingException {
    Connection conn = database.getConnection();
    String sql = "UPDATE task SET status = ?, name = ?, time = ?, pos = ?, target = ?, "
        + "target_user_num = ?, complete_user_num = ?, user_list = ? WHERE id = ?";
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, task.status);
      stmt.setString(2, task.name);
      stmt.setInt(3, task.time);
      stmt.setInt(4, task.pos);
      stmt.setString(5, toJson(task.target));
      stmt.setInt(6, task.targetUserNum);
      stmt.setInt(7, task.completeUserNum);
      stmt.setString(8, toJson(task.userList));
      stmt.setInt(9, task.id);
      return stmt.executeUpdate();
    }
  }

  public synchronized void saveReports(List<Report> reports, List<String> rawJson) throws SQLException {
    if (reports == null || reports.isEmpty()) {
      return;
    }
    Connection conn = database.getConnection();
    String sql = "INSERT OR REPLACE INTO report "
        + "(battle_id, wid, attack_name, garrison, attack_base_heroid, raw_json) "
        + "VALUES (?, ?, ?, ?, ?, ?)";
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      for (int i = 0; i < reports.size(); i++) {
        Report report = reports.get(i);
        stmt.setInt(1, report.battleId);
        stmt.setInt(2, report.wid);
        stmt.setString(3, report.attackName);
        stmt.setInt(4, report.garrison);
        stmt.setInt(5, report.attackBaseHeroid);
        stmt.setString(6, rawJson != null && rawJson.size() > i ? rawJson.get(i) : null);
        stmt.addBatch();
      }
      stmt.executeBatch();
    }
  }

  public synchronized long countReportsByWid(int wid) throws SQLException {
    return countByQuery("SELECT COUNT(*) FROM report WHERE wid = ?", wid);
  }

  public synchronized long countReportsByWidAndAttackName(int wid, String attackName) throws SQLException {
    return countByQuery("SELECT COUNT(*) FROM report WHERE wid = ? AND attack_name = ?", wid, attackName);
  }

  public synchronized long countReportsByWidAndAttackNameAndGarrison(int wid, String attackName, int garrison)
      throws SQLException {
    return countByQuery("SELECT COUNT(*) FROM report WHERE wid = ? AND attack_name = ? AND garrison = ?",
        wid, attackName, garrison);
  }

  public synchronized long countDistinctAttackBaseHero(int wid, String attackName, int garrison) throws SQLException {
    return countByQuery(
        "SELECT COUNT(DISTINCT attack_base_heroid) FROM report WHERE wid = ? AND attack_name = ? AND garrison = ?",
        wid, attackName, garrison);
  }

  public synchronized int deleteReportsByWid(int wid) throws SQLException {
    Connection conn = database.getConnection();
    try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM report WHERE wid = ?")) {
      stmt.setInt(1, wid);
      return stmt.executeUpdate();
    }
  }

  public synchronized void saveBattleReport(BattleReport report) throws SQLException {
    if (report == null) {
      return;
    }
    Connection conn = database.getConnection();
    String sql = "INSERT OR REPLACE INTO battle_report ("
        + "battle_id, attack_help_id, time, wid, wid_name, attack_name, attack_union_name, "
        + "attack_clan_name, defend_clan_name, attack_idu, defend_idu, defend_name, "
        + "defend_union_name, attack_advance, attack_all_hero_info, attacker_gear_info, "
        + "defend_advance, defend_all_hero_info, defender_gear_info, attack_hero_type, "
        + "attack_hero_type_advance, defend_hero_type, defend_hero_type_advance, "
        + "attack_hero1_id, attack_hero2_id, attack_hero3_id, attack_hero1_level, "
        + "attack_hero2_level, attack_hero3_level, attack_hero1_star, attack_hero2_star, "
        + "attack_hero3_star, attack_total_star, defend_hero1_id, defend_hero2_id, "
        + "defend_hero3_id, defend_hero1_level, defend_hero2_level, defend_hero3_level, "
        + "defend_hero1_star, defend_hero2_star, defend_hero3_star, defend_total_star, "
        + "attack_hp, defend_hp, npc, all_skill_info, result"
        + ") VALUES ("
        + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
        + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?"
        + ")";
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      int i = 1;
      stmt.setLong(i++, report.battleId);
      stmt.setString(i++, report.attackHelpId);
      stmt.setLong(i++, report.time);
      stmt.setString(i++, report.wid);
      stmt.setString(i++, report.widName);
      stmt.setString(i++, report.attackName);
      stmt.setString(i++, report.attackUnionName);
      stmt.setString(i++, report.attackClanName);
      stmt.setString(i++, report.defendClanName);
      stmt.setString(i++, report.attackIdu);
      stmt.setString(i++, report.defendIdu);
      stmt.setString(i++, report.defendName);
      stmt.setString(i++, report.defendUnionName);
      stmt.setString(i++, report.attackAdvance);
      stmt.setString(i++, report.attackAllHeroInfo);
      stmt.setString(i++, report.attackerGearInfo);
      stmt.setString(i++, report.defendAdvance);
      stmt.setString(i++, report.defendAllHeroInfo);
      stmt.setString(i++, report.defenderGearInfo);
      stmt.setString(i++, report.attackHeroType);
      stmt.setString(i++, report.attackHeroTypeAdvance);
      stmt.setString(i++, report.defendHeroType);
      stmt.setString(i++, report.defendHeroTypeAdvance);
      stmt.setLong(i++, report.attackHero1Id);
      stmt.setLong(i++, report.attackHero2Id);
      stmt.setLong(i++, report.attackHero3Id);
      stmt.setLong(i++, report.attackHero1Level);
      stmt.setLong(i++, report.attackHero2Level);
      stmt.setLong(i++, report.attackHero3Level);
      stmt.setLong(i++, report.attackHero1Star);
      stmt.setLong(i++, report.attackHero2Star);
      stmt.setLong(i++, report.attackHero3Star);
      stmt.setLong(i++, report.attackTotalStar);
      stmt.setLong(i++, report.defendHero1Id);
      stmt.setLong(i++, report.defendHero2Id);
      stmt.setLong(i++, report.defendHero3Id);
      stmt.setLong(i++, report.defendHero1Level);
      stmt.setLong(i++, report.defendHero2Level);
      stmt.setLong(i++, report.defendHero3Level);
      stmt.setLong(i++, report.defendHero1Star);
      stmt.setLong(i++, report.defendHero2Star);
      stmt.setLong(i++, report.defendHero3Star);
      stmt.setLong(i++, report.defendTotalStar);
      stmt.setLong(i++, report.attackHp);
      stmt.setLong(i++, report.defendHp);
      stmt.setLong(i++, report.npc);
      stmt.setString(i++, report.allSkillInfo);
      stmt.setLong(i++, report.result);
      stmt.executeUpdate();
    }
  }

  public synchronized List<Map<String, Object>> getGroupWuStats() throws SQLException {
    Connection conn = database.getConnection();
    String sql = "SELECT team_user.\"group\" AS group_name, "
        + "SUM(wu) AS total_wu, ROUND(AVG(wu)) AS average_wu, "
        + "IFNULL(sub.zero_wu_count, 0) AS zero_wu_count, COUNT(*) AS member_count "
        + "FROM team_user LEFT JOIN ("
        + "  SELECT \"group\", COUNT(*) AS zero_wu_count "
        + "  FROM team_user WHERE wu = 0 GROUP BY \"group\""
        + ") sub ON sub.\"group\" = team_user.\"group\" "
        + "GROUP BY team_user.\"group\" ORDER BY total_wu DESC";
    List<Map<String, Object>> stats = new ArrayList<>();
    try (PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        Map<String, Object> row = new HashMap<>();
        row.put("group", rs.getString("group_name"));
        row.put("member_count", rs.getInt("member_count"));
        row.put("total_wu", rs.getInt("total_wu"));
        row.put("average_wu", rs.getInt("average_wu"));
        row.put("zero_wu_count", rs.getInt("zero_wu_count"));
        stats.add(row);
      }
    }
    return stats;
  }

  public synchronized List<BattleReport> listBattleReports(BattleReportQuery query) throws SQLException {
    Connection conn = database.getConnection();
    List<Object> params = new ArrayList<>();
    StringBuilder sql = new StringBuilder("SELECT * FROM battle_report");
    List<String> conditions = new ArrayList<>();

    if (query.nextId > 0) {
      conditions.add("id < ?");
      params.add(query.nextId);
    }

    String type = query.type;
    if (type == null || type.isBlank() || "1".equals(type)) {
      appendReportFilters(conditions, params, query, true, true);
    } else if ("2".equals(type)) {
      appendReportFilters(conditions, params, query, true, false);
    } else if ("3".equals(type)) {
      appendReportFilters(conditions, params, query, false, true);
    } else if ("4".equals(type)) {
      appendReportFiltersStrict(conditions, params, query);
    }

    if (query.nonpc) {
      conditions.add("npc = 0");
    }

    if (!conditions.isEmpty()) {
      sql.append(" WHERE ").append(String.join(" AND ", conditions));
    }

    sql.append(" ORDER BY time DESC LIMIT 30");

    List<BattleReport> reports = new ArrayList<>();
    try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
      bindParams(stmt, params);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          reports.add(readBattleReport(rs));
        }
      }
    }
    return reports;
  }

  public synchronized long countBattleReports() throws SQLException {
    return countByQuery("SELECT COUNT(*) FROM battle_report");
  }

  public synchronized List<Map<String, Object>> getPlayerTeam(String name, String unionName, String idu)
      throws SQLException {
    Connection conn = database.getConnection();
    String likeName = "%" + (name == null ? "" : name) + "%";
    String likeUnion = "%" + (unionName == null ? "" : unionName) + "%";
    String likeIdu = "%" + (idu == null ? "" : idu) + "%";

    String sql = "WITH ranked_data AS ("
        + " SELECT attack_name AS player_name, attack_hero1_id AS hero1_id, "
        + " attack_hero2_id AS hero2_id, attack_hero3_id AS hero3_id, "
        + " attack_hero1_level AS hero1_level, attack_hero2_level AS hero2_level, "
        + " attack_hero3_level AS hero3_level, attack_hero1_star AS hero1_star, "
        + " attack_hero2_star AS hero2_star, attack_hero3_star AS hero3_star, "
        + " attack_total_star AS total_star, attack_hp AS hp, "
        + " attacker_gear_info AS gear, attack_hero_type AS hero_type, "
        + " attack_idu AS idu, time, all_skill_info, battle_id, "
        + " 'attack' AS role, ROW_NUMBER() OVER ("
        + "   PARTITION BY attack_name, attack_hero1_id "
        + "   ORDER BY attack_hero1_level DESC, time DESC"
        + " ) AS rn "
        + " FROM battle_report "
        + " WHERE attack_hero1_id != 0 AND attack_hero2_id != 0 AND attack_hero3_id != 0 "
        + " AND attack_hero1_level >= 15 AND attack_hero2_level >= 15 AND attack_hero3_level >= 15 "
        + " AND attack_hp >= 10000 "
        + " AND attack_name LIKE ? AND attack_union_name LIKE ? AND attack_idu LIKE ? "
        + " AND npc = 0 AND all_skill_info != '' AND all_skill_info IS NOT NULL "
        + " UNION ALL "
        + " SELECT defend_name AS player_name, defend_hero1_id AS hero1_id, "
        + " defend_hero2_id AS hero2_id, defend_hero3_id AS hero3_id, "
        + " defend_hero1_level AS hero1_level, defend_hero2_level AS hero2_level, "
        + " defend_hero3_level AS hero3_level, defend_hero1_star AS hero1_star, "
        + " defend_hero2_star AS hero2_star, defend_hero3_star AS hero3_star, "
        + " defend_total_star AS total_star, defend_hp AS hp, "
        + " defender_gear_info AS gear, defend_hero_type AS hero_type, "
        + " defend_idu AS idu, time, all_skill_info, battle_id, "
        + " 'defend' AS role, ROW_NUMBER() OVER ("
        + "   PARTITION BY defend_name, defend_hero1_id "
        + "   ORDER BY defend_hero1_level DESC, time DESC"
        + " ) AS rn "
        + " FROM battle_report "
        + " WHERE defend_hero1_id != 0 AND defend_hero2_id != 0 AND defend_hero3_id != 0 "
        + " AND defend_hero1_level >= 15 AND defend_hero2_level >= 15 AND defend_hero3_level >= 15 "
        + " AND defend_hp >= 10000 "
        + " AND defend_name LIKE ? AND defend_union_name LIKE ? AND defend_idu LIKE ? "
        + " AND npc = 0 AND all_skill_info != '' AND all_skill_info IS NOT NULL "
        + "), deduplicated_data AS ("
        + " SELECT player_name, hero1_id, hero2_id, hero3_id, hero1_level, hero2_level, hero3_level, "
        + " hero1_star, hero2_star, hero3_star, total_star, hp, gear, hero_type, idu, time, "
        + " all_skill_info, battle_id, role, ROW_NUMBER() OVER ("
        + "   PARTITION BY player_name, hero1_id, hero2_id, hero3_id "
        + "   ORDER BY time DESC"
        + " ) AS dedup_rn "
        + " FROM ranked_data WHERE rn = 1"
        + ") "
        + "SELECT player_name, hero1_id, hero2_id, hero3_id, hero1_level, hero2_level, hero3_level, "
        + " hero1_star, hero2_star, hero3_star, total_star, hp, gear, hero_type, idu, time, "
        + " all_skill_info, battle_id, role "
        + "FROM deduplicated_data WHERE dedup_rn = 1 ORDER BY player_name, time DESC";

    List<Map<String, Object>> results = new ArrayList<>();
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, likeName);
      stmt.setString(2, likeUnion);
      stmt.setString(3, likeIdu);
      stmt.setString(4, likeName);
      stmt.setString(5, likeUnion);
      stmt.setString(6, likeIdu);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          Map<String, Object> row = new HashMap<>();
          row.put("player_name", rs.getString("player_name"));
          row.put("hero1_id", rs.getInt("hero1_id"));
          row.put("hero2_id", rs.getInt("hero2_id"));
          row.put("hero3_id", rs.getInt("hero3_id"));
          row.put("hero1_level", rs.getInt("hero1_level"));
          row.put("hero2_level", rs.getInt("hero2_level"));
          row.put("hero3_level", rs.getInt("hero3_level"));
          row.put("hero1_star", rs.getInt("hero1_star"));
          row.put("hero2_star", rs.getInt("hero2_star"));
          row.put("hero3_star", rs.getInt("hero3_star"));
          row.put("total_star", rs.getInt("total_star"));
          row.put("hp", rs.getInt("hp"));
          row.put("gear", rs.getString("gear"));
          row.put("hero_type", rs.getString("hero_type"));
          row.put("idu", rs.getString("idu"));
          row.put("time", rs.getLong("time"));
          row.put("all_skill_info", rs.getString("all_skill_info"));
          row.put("battle_id", rs.getLong("battle_id"));
          row.put("role", rs.getString("role"));
          results.add(row);
        }
      }
    }
    return results;
  }

  private Task readTask(ResultSet rs) throws SQLException {
    Task task = new Task();
    task.id = rs.getInt("id");
    task.status = rs.getInt("status");
    task.name = rs.getString("name");
    task.time = rs.getInt("time");
    task.pos = rs.getInt("pos");
    task.target = fromJsonList(rs.getString("target"));
    task.targetUserNum = rs.getInt("target_user_num");
    task.completeUserNum = rs.getInt("complete_user_num");
    task.userList = fromJsonUserList(rs.getString("user_list"));
    return task;
  }

  private BattleReport readBattleReport(ResultSet rs) throws SQLException {
    BattleReport report = new BattleReport();
    report.id = rs.getLong("id");
    report.battleId = rs.getLong("battle_id");
    report.attackHelpId = rs.getString("attack_help_id");
    report.time = rs.getLong("time");
    report.wid = rs.getString("wid");
    report.widName = rs.getString("wid_name");
    report.attackName = rs.getString("attack_name");
    report.attackUnionName = rs.getString("attack_union_name");
    report.attackClanName = rs.getString("attack_clan_name");
    report.defendClanName = rs.getString("defend_clan_name");
    report.attackIdu = rs.getString("attack_idu");
    report.defendIdu = rs.getString("defend_idu");
    report.defendName = rs.getString("defend_name");
    report.defendUnionName = rs.getString("defend_union_name");
    report.attackAdvance = rs.getString("attack_advance");
    report.attackAllHeroInfo = rs.getString("attack_all_hero_info");
    report.attackerGearInfo = rs.getString("attacker_gear_info");
    report.defendAdvance = rs.getString("defend_advance");
    report.defendAllHeroInfo = rs.getString("defend_all_hero_info");
    report.defenderGearInfo = rs.getString("defender_gear_info");
    report.attackHeroType = rs.getString("attack_hero_type");
    report.attackHeroTypeAdvance = rs.getString("attack_hero_type_advance");
    report.defendHeroType = rs.getString("defend_hero_type");
    report.defendHeroTypeAdvance = rs.getString("defend_hero_type_advance");
    report.attackHero1Id = rs.getLong("attack_hero1_id");
    report.attackHero2Id = rs.getLong("attack_hero2_id");
    report.attackHero3Id = rs.getLong("attack_hero3_id");
    report.attackHero1Level = rs.getLong("attack_hero1_level");
    report.attackHero2Level = rs.getLong("attack_hero2_level");
    report.attackHero3Level = rs.getLong("attack_hero3_level");
    report.attackHero1Star = rs.getLong("attack_hero1_star");
    report.attackHero2Star = rs.getLong("attack_hero2_star");
    report.attackHero3Star = rs.getLong("attack_hero3_star");
    report.attackTotalStar = rs.getLong("attack_total_star");
    report.defendHero1Id = rs.getLong("defend_hero1_id");
    report.defendHero2Id = rs.getLong("defend_hero2_id");
    report.defendHero3Id = rs.getLong("defend_hero3_id");
    report.defendHero1Level = rs.getLong("defend_hero1_level");
    report.defendHero2Level = rs.getLong("defend_hero2_level");
    report.defendHero3Level = rs.getLong("defend_hero3_level");
    report.defendHero1Star = rs.getLong("defend_hero1_star");
    report.defendHero2Star = rs.getLong("defend_hero2_star");
    report.defendHero3Star = rs.getLong("defend_hero3_star");
    report.defendTotalStar = rs.getLong("defend_total_star");
    report.attackHp = rs.getLong("attack_hp");
    report.defendHp = rs.getLong("defend_hp");
    report.npc = rs.getLong("npc");
    report.allSkillInfo = rs.getString("all_skill_info");
    report.result = rs.getLong("result");
    return report;
  }

  private String toJson(Object value) throws JsonProcessingException {
    if (value == null) {
      return null;
    }
    return mapper.writeValueAsString(value);
  }

  private List<String> fromJsonList(String json) {
    if (json == null || json.isBlank()) {
      return Collections.emptyList();
    }
    try {
      return mapper.readValue(json, new TypeReference<List<String>>() {});
    } catch (Exception e) {
      return Collections.emptyList();
    }
  }

  private Map<Integer, TaskUserList> fromJsonUserList(String json) {
    if (json == null || json.isBlank()) {
      return new HashMap<>();
    }
    try {
      Map<String, TaskUserList> raw = mapper.readValue(
          json, new TypeReference<Map<String, TaskUserList>>() {});
      Map<Integer, TaskUserList> result = new HashMap<>();
      for (Map.Entry<String, TaskUserList> entry : raw.entrySet()) {
        try {
          result.put(Integer.parseInt(entry.getKey()), entry.getValue());
        } catch (NumberFormatException e) {
          // ignore invalid key
        }
      }
      return result;
    } catch (Exception e) {
      return new HashMap<>();
    }
  }

  private long countByQuery(String sql, Object... params) throws SQLException {
    Connection conn = database.getConnection();
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      for (int i = 0; i < params.length; i++) {
        stmt.setObject(i + 1, params[i]);
      }
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getLong(1);
        }
      }
    }
    return 0;
  }

  private void bindParams(PreparedStatement stmt, List<Object> params) throws SQLException {
    for (int i = 0; i < params.size(); i++) {
      stmt.setObject(i + 1, params.get(i));
    }
  }

  private void appendReportFilters(List<String> conditions, List<Object> params,
      BattleReportQuery query, boolean includeAttack, boolean includeDefend) {
    if (query.atkName != null && !query.atkName.isBlank()) {
      if (includeAttack && includeDefend) {
        conditions.add("(attack_name LIKE ? OR defend_name LIKE ?)");
        params.add("%" + query.atkName + "%");
        params.add("%" + query.atkName + "%");
      } else if (includeAttack) {
        conditions.add("attack_name LIKE ?");
        params.add("%" + query.atkName + "%");
      } else {
        conditions.add("defend_name LIKE ?");
        params.add("%" + query.atkName + "%");
      }
    }

    if (query.atkUnionName != null && !query.atkUnionName.isBlank()) {
      if (includeAttack && includeDefend) {
        conditions.add("(attack_union_name LIKE ? OR defend_union_name LIKE ?)");
        params.add("%" + query.atkUnionName + "%");
        params.add("%" + query.atkUnionName + "%");
      } else if (includeAttack) {
        conditions.add("attack_union_name LIKE ?");
        params.add("%" + query.atkUnionName + "%");
      } else {
        conditions.add("defend_union_name LIKE ?");
        params.add("%" + query.atkUnionName + "%");
      }
    }

    if (query.atkHp != null && !query.atkHp.isBlank()) {
      if (includeAttack && includeDefend) {
        conditions.add("(attack_hp >= ? OR defend_hp >= ?)");
        params.add(query.atkHp);
        params.add(query.atkHp);
      } else if (includeAttack) {
        conditions.add("attack_hp >= ?");
        params.add(query.atkHp);
      } else {
        conditions.add("defend_hp >= ?");
        params.add(query.atkHp);
      }
    }

    if (query.atkLevel != null && !query.atkLevel.isBlank()) {
      if (includeAttack && includeDefend) {
        conditions.add("((attack_hero1_level >= ? AND attack_hero2_level >= ? "
            + "AND attack_hero3_level >= ?) OR (defend_hero1_level >= ? "
            + "AND defend_hero2_level >= ? AND defend_hero3_level >= ?))");
        for (int i = 0; i < 6; i++) {
          params.add(query.atkLevel);
        }
      } else if (includeAttack) {
        conditions.add("(attack_hero1_level >= ? AND attack_hero2_level >= ? "
            + "AND attack_hero3_level >= ?)");
        for (int i = 0; i < 3; i++) {
          params.add(query.atkLevel);
        }
      } else {
        conditions.add("(defend_hero1_level >= ? AND defend_hero2_level >= ? "
            + "AND defend_hero3_level >= ?)");
        for (int i = 0; i < 3; i++) {
          params.add(query.atkLevel);
        }
      }
    }

    if (query.atkStar != null && !query.atkStar.isBlank()) {
      if (includeAttack && includeDefend) {
        conditions.add("(attack_total_star >= ? OR defend_total_star >= ?)");
        params.add(query.atkStar);
        params.add(query.atkStar);
      } else if (includeAttack) {
        conditions.add("attack_total_star >= ?");
        params.add(query.atkStar);
      } else {
        conditions.add("defend_total_star >= ?");
        params.add(query.atkStar);
      }
    }
  }

  private void appendReportFiltersStrict(List<String> conditions, List<Object> params,
      BattleReportQuery query) {
    if (query.atkName != null && !query.atkName.isBlank()) {
      conditions.add("(attack_name LIKE ? OR defend_name LIKE ?)");
      params.add("%" + query.atkName + "%");
      params.add("%" + query.atkName + "%");
    }

    if (query.atkUnionName != null && !query.atkUnionName.isBlank()) {
      conditions.add("(attack_union_name LIKE ? OR defend_union_name LIKE ?)");
      params.add("%" + query.atkUnionName + "%");
      params.add("%" + query.atkUnionName + "%");
    }

    if (query.atkHp != null && !query.atkHp.isBlank()) {
      conditions.add("(attack_hp >= ? AND defend_hp >= ?)");
      params.add(query.atkHp);
      params.add(query.atkHp);
    }

    if (query.atkLevel != null && !query.atkLevel.isBlank()) {
      conditions.add("((attack_hero1_level >= ? AND attack_hero2_level >= ? "
          + "AND attack_hero3_level >= ?) AND (defend_hero1_level >= ? "
          + "AND defend_hero2_level >= ? AND defend_hero3_level >= ?))");
      for (int i = 0; i < 6; i++) {
        params.add(query.atkLevel);
      }
    }

    if (query.atkStar != null && !query.atkStar.isBlank()) {
      conditions.add("(attack_total_star >= ? AND defend_total_star >= ?)");
      params.add(query.atkStar);
      params.add(query.atkStar);
    }
  }

  public static class BattleReportQuery {
    public int nextId;
    public String atkName;
    public String atkUnionName;
    public String atkHp;
    public String atkLevel;
    public String atkStar;
    public String type;
    public boolean nonpc;
  }
}
