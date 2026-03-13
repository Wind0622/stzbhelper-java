package stzbhelper.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BattleReport {
  @JsonProperty("id")
  public long id;

  @JsonProperty("battle_id")
  public long battleId;

  @JsonProperty("attack_help_id")
  public String attackHelpId;

  @JsonProperty("time")
  public long time;

  @JsonProperty("wid")
  public String wid;

  @JsonProperty("wid_name")
  public String widName;

  @JsonProperty("attack_name")
  public String attackName;

  @JsonProperty("attack_union_name")
  public String attackUnionName;

  @JsonProperty("attack_clan_name")
  public String attackClanName;

  @JsonProperty("defend_clan_name")
  public String defendClanName;

  @JsonProperty("attack_idu")
  public String attackIdu;

  @JsonProperty("defend_idu")
  public String defendIdu;

  @JsonProperty("defend_name")
  public String defendName;

  @JsonProperty("defend_union_name")
  public String defendUnionName;

  @JsonProperty("attack_advance")
  public String attackAdvance;

  @JsonProperty("attack_all_hero_info")
  public String attackAllHeroInfo;

  @JsonProperty("attacker_gear_info")
  public String attackerGearInfo;

  @JsonProperty("defend_advance")
  public String defendAdvance;

  @JsonProperty("defend_all_hero_info")
  public String defendAllHeroInfo;

  @JsonProperty("defender_gear_info")
  public String defenderGearInfo;

  @JsonProperty("attack_hero_type")
  public String attackHeroType;

  @JsonProperty("attack_hero_type_advance")
  public String attackHeroTypeAdvance;

  @JsonProperty("defend_hero_type")
  public String defendHeroType;

  @JsonProperty("defend_hero_type_advance")
  public String defendHeroTypeAdvance;

  @JsonProperty("attack_hero1_id")
  public long attackHero1Id;

  @JsonProperty("attack_hero2_id")
  public long attackHero2Id;

  @JsonProperty("attack_hero3_id")
  public long attackHero3Id;

  @JsonProperty("attack_hero1_level")
  public long attackHero1Level;

  @JsonProperty("attack_hero2_level")
  public long attackHero2Level;

  @JsonProperty("attack_hero3_level")
  public long attackHero3Level;

  @JsonProperty("attack_hero1_star")
  public long attackHero1Star;

  @JsonProperty("attack_hero2_star")
  public long attackHero2Star;

  @JsonProperty("attack_hero3_star")
  public long attackHero3Star;

  @JsonProperty("attack_total_star")
  public long attackTotalStar;

  @JsonProperty("defend_hero1_id")
  public long defendHero1Id;

  @JsonProperty("defend_hero2_id")
  public long defendHero2Id;

  @JsonProperty("defend_hero3_id")
  public long defendHero3Id;

  @JsonProperty("defend_hero1_level")
  public long defendHero1Level;

  @JsonProperty("defend_hero2_level")
  public long defendHero2Level;

  @JsonProperty("defend_hero3_level")
  public long defendHero3Level;

  @JsonProperty("defend_hero1_star")
  public long defendHero1Star;

  @JsonProperty("defend_hero2_star")
  public long defendHero2Star;

  @JsonProperty("defend_hero3_star")
  public long defendHero3Star;

  @JsonProperty("defend_total_star")
  public long defendTotalStar;

  @JsonProperty("attack_hp")
  public long attackHp;

  @JsonProperty("defend_hp")
  public long defendHp;

  @JsonProperty("npc")
  public long npc;

  @JsonProperty("all_skill_info")
  public String allSkillInfo;

  @JsonProperty("result")
  public long result;
}
