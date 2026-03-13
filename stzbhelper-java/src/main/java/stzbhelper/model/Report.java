package stzbhelper.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Report {
  @JsonProperty("battle_id")
  public int battleId;

  @JsonProperty("wid")
  public int wid;

  @JsonProperty("attack_name")
  public String attackName;

  @JsonProperty("garrison")
  public int garrison;

  @JsonProperty("attack_base_heroid")
  public int attackBaseHeroid;
}
