package stzbhelper.web;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiResponse {
  @JsonProperty("code")
  public int code;

  @JsonProperty("msg")
  public String message;

  @JsonProperty("data")
  public Object data;

  public static ApiResponse success(Object data) {
    ApiResponse response = new ApiResponse();
    response.code = 200;
    response.message = "ok";
    response.data = data;
    return response;
  }

  public static ApiResponse successMessage(String message, Object data) {
    ApiResponse response = new ApiResponse();
    response.code = 200;
    response.message = message == null || message.isBlank() ? "ok" : message;
    response.data = data;
    return response;
  }

  public static ApiResponse error(String message) {
    ApiResponse response = new ApiResponse();
    response.code = 500;
    response.message = message == null || message.isBlank() ? "error" : message;
    response.data = null;
    return response;
  }
}
