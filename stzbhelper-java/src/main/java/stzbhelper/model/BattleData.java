package stzbhelper.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BattleData {
  @JsonProperty("battle_id")
  public long battleId;

  @JsonProperty("attack_help_id")
  public String attackHelpId;

  @JsonProperty("time")
  public long time;

  @JsonProperty("wid")
  public Object wid;

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

  @JsonProperty("attack_idu")
  public String attackIdu;

  @JsonProperty("defend_idu")
  public String defendIdu;
}
