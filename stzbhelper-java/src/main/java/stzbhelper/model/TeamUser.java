package stzbhelper.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class TeamUser {
  @JsonProperty("id")
  public int id;

  @JsonProperty("name")
  public String name;

  @JsonProperty("contribute_total")
  public int contributeTotal;

  @JsonProperty("contribute_week")
  public int contributeWeek;

  @JsonProperty("pos")
  public int pos;

  @JsonProperty("power")
  public int power;

  @JsonProperty("wu")
  public int wu;

  @JsonProperty("group")
  public String group;

  @JsonProperty("join_time")
  public int joinTime;

  public static TeamUser fromRawList(List<Object> data) {
    if (data == null || data.size() < 31) {
      return null;
    }
    Object groupValue = data.get(13);
    String group = groupValue == null ? "" : String.valueOf(groupValue);
    if (group.isEmpty()) {
      group = "未分组";
    }

    TeamUser user = new TeamUser();
    user.id = toInt(data.get(0));
    user.name = toString(data.get(1));
    user.contributeTotal = toInt(data.get(2));
    user.contributeWeek = toInt(data.get(7));
    user.pos = toInt(data.get(6));
    user.power = toInt(data.get(8));
    user.wu = toInt(data.get(10));
    user.group = group;
    user.joinTime = toInt(data.get(30));
    return user;
  }

  private static int toInt(Object value) {
    if (value == null) {
      return 0;
    }
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    try {
      return Integer.parseInt(String.valueOf(value));
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private static String toString(Object value) {
    return value == null ? "" : String.valueOf(value);
  }
}
