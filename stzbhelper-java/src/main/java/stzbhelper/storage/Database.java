package stzbhelper.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
  private Connection connection;

  public synchronized void init(String dbFile) throws SQLException {
    String filename = normalizeFilename(dbFile);
    connection = DriverManager.getConnection("jdbc:sqlite:" + filename);
    initSchema();
  }

  public synchronized void switchTo(String dbFile) throws SQLException {
    closeQuietly();
    init(dbFile);
  }

  public synchronized Connection getConnection() {
    return connection;
  }

  private String normalizeFilename(String dbFile) {
    if (dbFile == null || dbFile.isBlank()) {
      return "stzbhelper.db";
    }
    if (dbFile.endsWith(".db")) {
      return dbFile;
    }
    return dbFile + ".db";
  }

  private void initSchema() throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      stmt.execute("""
          CREATE TABLE IF NOT EXISTS team_user (
            id INTEGER PRIMARY KEY,
            name TEXT,
            contribute_total INTEGER,
            contribute_week INTEGER,
            pos INTEGER,
            power INTEGER,
            wu INTEGER,
            "group" TEXT,
            join_time INTEGER
          )
          """);

      stmt.execute("""
          CREATE TABLE IF NOT EXISTS task (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            status INTEGER,
            name TEXT,
            time INTEGER,
            pos INTEGER,
            target TEXT,
            target_user_num INTEGER,
            complete_user_num INTEGER,
            user_list TEXT
          )
          """);

      stmt.execute("""
          CREATE TABLE IF NOT EXISTS report (
            battle_id INTEGER PRIMARY KEY,
            wid INTEGER,
            attack_name TEXT,
            garrison INTEGER,
            attack_base_heroid INTEGER,
            raw_json TEXT
          )
          """);

      stmt.execute("""
          CREATE TABLE IF NOT EXISTS battle_report (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            battle_id INTEGER UNIQUE,
            attack_help_id TEXT,
            time INTEGER,
            wid TEXT,
            wid_name TEXT,
            attack_name TEXT,
            attack_union_name TEXT,
            attack_clan_name TEXT,
            defend_clan_name TEXT,
            attack_idu TEXT,
            defend_idu TEXT,
            defend_name TEXT,
            defend_union_name TEXT,
            attack_advance TEXT,
            attack_all_hero_info TEXT,
            attacker_gear_info TEXT,
            defend_advance TEXT,
            defend_all_hero_info TEXT,
            defender_gear_info TEXT,
            attack_hero_type TEXT,
            attack_hero_type_advance TEXT,
            defend_hero_type TEXT,
            defend_hero_type_advance TEXT,
            attack_hero1_id INTEGER,
            attack_hero2_id INTEGER,
            attack_hero3_id INTEGER,
            attack_hero1_level INTEGER,
            attack_hero2_level INTEGER,
            attack_hero3_level INTEGER,
            attack_hero1_star INTEGER,
            attack_hero2_star INTEGER,
            attack_hero3_star INTEGER,
            attack_total_star INTEGER,
            defend_hero1_id INTEGER,
            defend_hero2_id INTEGER,
            defend_hero3_id INTEGER,
            defend_hero1_level INTEGER,
            defend_hero2_level INTEGER,
            defend_hero3_level INTEGER,
            defend_hero1_star INTEGER,
            defend_hero2_star INTEGER,
            defend_hero3_star INTEGER,
            defend_total_star INTEGER,
            attack_hp INTEGER,
            defend_hp INTEGER,
            npc INTEGER,
            all_skill_info TEXT,
            result INTEGER
          )
          """);
    }
  }

  private void closeQuietly() {
    if (connection != null) {
      try {
        connection.close();
      } catch (SQLException ignored) {
        // ignore
      }
    }
  }
}
