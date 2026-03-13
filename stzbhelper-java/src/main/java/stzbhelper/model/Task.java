package stzbhelper.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class Task {
  @JsonProperty("id")
  public int id;

  @JsonProperty("status")
  public int status;

  @JsonProperty("name")
  public String name;

  @JsonProperty("time")
  public int time;

  @JsonProperty("pos")
  public int pos;

  @JsonProperty("target")
  public List<String> target;

  @JsonProperty("target_user_num")
  public int targetUserNum;

  @JsonProperty("complete_user_num")
  public int completeUserNum;

  @JsonProperty("user_list")
  public Map<Integer, TaskUserList> userList;
}
