package stzbhelper.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TaskUserList {
  @JsonProperty("id")
  public int id;

  @JsonProperty("name")
  public String name;

  @JsonProperty("group")
  public String group;

  @JsonProperty("atk_num")
  public int atkNum;

  @JsonProperty("dis_num")
  public int disNum;

  @JsonProperty("atk_team_num")
  public int atkTeamNum;

  @JsonProperty("dis_team_num")
  public int disTeamNum;
}
