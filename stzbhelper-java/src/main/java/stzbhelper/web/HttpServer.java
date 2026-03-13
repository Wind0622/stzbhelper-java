package stzbhelper.web;

import io.javalin.Javalin;
import io.javalin.http.Handler;
import io.javalin.http.staticfiles.Location;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import stzbhelper.global.GlobalState;
import stzbhelper.model.PlayerTeam;
import stzbhelper.model.Task;
import stzbhelper.model.TaskUserList;
import stzbhelper.model.TeamUser;
import stzbhelper.storage.StorageService;
import stzbhelper.storage.StorageService.BattleReportQuery;

public class HttpServer {
  private final StorageService storage;
  private Javalin app;

  public HttpServer(StorageService storage) {
    this.storage = storage;
  }

  public void start(int port) {
    app = Javalin.create(config -> {
      config.showJavalinBanner = false;
      config.staticFiles.add(staticFiles -> {
        staticFiles.directory = "/web/dist";
        staticFiles.hostedPath = "/";
        staticFiles.location = Location.CLASSPATH;
      });
    });

    app.get("/", ctx -> ctx.redirect("/index.html"));
    app.get("/data.html", ctx -> ctx.redirect("/index.html"));
    
    app.before("/v1/*", ctx -> {
      System.out.println("API Request: " + ctx.method() + " " + ctx.path());
    });

    registerRoutes();
    
    app.error(404, ctx -> {
      if (!ctx.path().startsWith("/v1/")) {
        ctx.redirect("/index.html");
      } else {
        ctx.json(ApiResponse.error("404 - API Not Found"));
      }
    });
    
    app.start(port);
  }

  private void registerRoutes() {
    addAny("/v1/getTeamUser", wrap(ctx -> {
      String group = ctx.queryParam("group");
      List<TeamUser> users = storage.findTeamUsers(group);
      System.out.println("API Response: getTeamUser returned " + users.size() + " records");
      ctx.json(ApiResponse.success(users));
    }));

    addAny("/v1/stzb/player/team/get", wrap(ctx -> {
      List<PlayerTeam> teams = storage.getPlayerTeams();
      System.out.println("API Response: getPlayerTeams returned " + teams.size() + " records");
      ctx.json(ApiResponse.success(teams));
    }));

    addAny("/v1/getTeamGroup", wrap(ctx -> {
      ctx.json(ApiResponse.success(storage.findTeamGroups()));
    }));

    addAny("/v1/getTaskList", wrap(ctx -> {
      ctx.json(ApiResponse.success(storage.getTaskList(true)));
    }));

    addAny("/v1/getTask/{tid}", wrap(ctx -> {
      int tid = parseInt(ctx.pathParam("tid"));
      Task task = storage.getTaskById(tid);
      if (task == null) {
        ctx.json(ApiResponse.error("获取任务失败"));
        return;
      }
      ctx.json(ApiResponse.success(task));
    }));

    app.post("/v1/createTask", wrap(ctx -> {
      String taskName = ctx.formParam("taskname");
      String taskTime = ctx.formParam("tasktime");
      List<String> targetGroup = ctx.formParams("targetgroup");
      List<String> taskPos = ctx.formParams("taskpos");

      int taskPosFormat = toTaskPos(taskPos);
      if (taskPosFormat == 0) {
        ctx.json(ApiResponse.error("任务坐标格式错误"));
        return;
      }

      int taskTimeFormat = parseInt(taskTime);
      if (taskTimeFormat == 0) {
        ctx.json(ApiResponse.error("任务时间格式错误"));
        return;
      }

      List<TeamUser> users = storage.findTeamUsersByGroups(targetGroup);
      if (users.isEmpty()) {
        ctx.json(ApiResponse.error("创建出错:目标人数为0"));
        return;
      }

      Map<Integer, TaskUserList> userList = teamUserListToTaskUserList(users);
      Task task = new Task();
      task.status = 0;
      task.name = taskName;
      task.time = taskTimeFormat;
      task.pos = taskPosFormat;
      task.target = targetGroup;
      task.targetUserNum = users.size();
      task.completeUserNum = 0;
      task.userList = userList;

      int newId = storage.createTask(task);
      if (newId > 0) {
        ctx.json(ApiResponse.successMessage("创建成功", newId));
      } else {
        ctx.json(ApiResponse.error("创建失败"));
      }
    }));

    addAny("/v1/deleteTask/{tid}", wrap(ctx -> {
      int tid = parseInt(ctx.pathParam("tid"));
      if (tid == 0) {
        ctx.json(ApiResponse.error("任务ID错误"));
        return;
      }
      int rows = storage.deleteTask(tid);
      if (rows > 0) {
        ctx.json(ApiResponse.successMessage("删除成功", rows));
      } else {
        ctx.json(ApiResponse.error("删除失败"));
      }
    }));

    app.post("/v1/enable/getReport", wrap(ctx -> {
      int pos = parseInt(ctx.formParam("pos"));
      if (pos == 0) {
        ctx.json(ApiResponse.error("坐标格式错误"));
        return;
      }
      GlobalState.exVar.neededReportPos = pos;
      GlobalState.exVar.needGetReport = true;
      ctx.json(ApiResponse.success(null));
    }));

    addAny("/v1/disable/getReport", wrap(ctx -> {
      GlobalState.exVar.neededReportPos = 0;
      GlobalState.exVar.needGetReport = false;
      ctx.json(ApiResponse.success(null));
    }));

    addAny("/v1/getReportNumByTaskId/{tid}", wrap(ctx -> {
      int tid = parseInt(ctx.pathParam("tid"));
      if (tid == 0) {
        ctx.json(ApiResponse.error("任务ID错误"));
        return;
      }
      Task task = storage.getTaskById(tid);
      if (task == null) {
        ctx.json(ApiResponse.error("获取任务失败"));
        return;
      }
      long count = storage.countReportsByWid(task.pos);
      Map<String, Object> data = new HashMap<>();
      data.put("count", count);
      ctx.json(ApiResponse.success(data));
    }));

    addAny("/v1/statisticsReport/{tid}", wrap(ctx -> {
      int tid = parseInt(ctx.pathParam("tid"));
      if (tid == 0) {
        ctx.json(ApiResponse.error("任务ID错误"));
        return;
      }
      Task task = storage.getTaskById(tid);
      if (task == null) {
        ctx.json(ApiResponse.error("获取任务失败"));
        return;
      }
      task.completeUserNum = 0;
      if (task.userList != null) {
        for (Map.Entry<Integer, TaskUserList> entry : task.userList.entrySet()) {
          TaskUserList user = entry.getValue();
          long num = storage.countReportsByWidAndAttackName(task.pos, user.name);
          long atkNum = storage.countReportsByWidAndAttackNameAndGarrison(task.pos, user.name, 0);
          long disNum = storage.countReportsByWidAndAttackNameAndGarrison(task.pos, user.name, 1);
          long atkTeamNum = storage.countDistinctAttackBaseHero(task.pos, user.name, 0);
          long disTeamNum = storage.countDistinctAttackBaseHero(task.pos, user.name, 1);
          user.atkNum = (int) atkNum;
          user.disNum = (int) disNum;
          user.atkTeamNum = (int) atkTeamNum;
          user.disTeamNum = (int) disTeamNum;
          if (num != 0) {
            task.completeUserNum++;
          }
        }
      }
      int rows = storage.updateTask(task);
      if (rows > 0) {
        ctx.json(ApiResponse.successMessage("统计考勤数据成功", rows));
      } else {
        ctx.json(ApiResponse.error("统计考勤数据失败"));
      }
    }));

    addAny("/v1/getGroupWu", wrap(ctx -> {
      ctx.json(ApiResponse.success(storage.getGroupWuStats()));
    }));

    addAny("/v1/deleteTaskReport/{tid}", wrap(ctx -> {
      int tid = parseInt(ctx.pathParam("tid"));
      if (tid == 0) {
        ctx.json(ApiResponse.error("任务ID错误"));
        return;
      }
      Task task = storage.getTaskById(tid);
      if (task == null) {
        ctx.json(ApiResponse.error("清理任务战报失败"));
        return;
      }
      int rows = storage.deleteReportsByWid(task.pos);
      if (rows > 0) {
        ctx.json(ApiResponse.successMessage("清理战报成功", rows));
      } else {
        ctx.json(ApiResponse.error("清理失败,可能战报已清理"));
      }
    }));

    app.get("/v1/stzb/report/list", wrap(ctx -> {
      String nextid = ctx.queryParam("nextid");
      if (nextid == null || nextid.isBlank()) {
        ctx.json(ApiResponse.error("参数错误"));
        return;
      }
      BattleReportQuery query = new BattleReportQuery();
      query.nextId = parseInt(nextid);
      query.atkName = ctx.queryParam("atkname");
      query.atkUnionName = ctx.queryParam("atkunionname");
      query.atkHp = ctx.queryParam("atkhp");
      query.atkLevel = ctx.queryParam("atklevel");
      query.atkStar = ctx.queryParam("atkstar");
      query.type = ctx.queryParam("type");
      query.nonpc = "1".equals(ctx.queryParam("nonpc"));

      List<?> reportList = storage.listBattleReports(query);
      long total = storage.countBattleReports();
      Map<String, Object> data = new HashMap<>();
      data.put("report", reportList);
      data.put("total", total);
      ctx.json(ApiResponse.success(data));
    }));

    addAny("/v1/enable/getBattleReport", wrap(ctx -> {
      GlobalState.exVar.needGetBattleData = true;
      ctx.json(ApiResponse.success(null));
    }));

    addAny("/v1/disable/getBattleReport", wrap(ctx -> {
      GlobalState.exVar.needGetBattleData = false;
      ctx.json(ApiResponse.success(null));
    }));
  }

  private Handler wrap(Handler handler) {
    return ctx -> {
      try {
        handler.handle(ctx);
      } catch (Exception e) {
        ctx.json(ApiResponse.error("操作失败"));
      }
    };
  }

  private void addAny(String path, Handler handler) {
    app.get(path, handler);
    app.post(path, handler);
    app.put(path, handler);
    app.delete(path, handler);
    app.patch(path, handler);
  }

  private int parseInt(String value) {
    if (value == null || value.isBlank()) {
      return 0;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private int toTaskPos(List<String> pos) {
    if (pos == null || pos.size() != 2) {
      return 0;
    }
    try {
      int part1 = Integer.parseInt(pos.get(0));
      int part2 = Integer.parseInt(pos.get(1));
      String part2Str = String.format("%04d", part2);
      return Integer.parseInt(part1 + part2Str);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private Map<Integer, TaskUserList> teamUserListToTaskUserList(List<TeamUser> users) {
    Map<Integer, TaskUserList> map = new HashMap<>();
    for (TeamUser user : users) {
      TaskUserList taskUser = new TaskUserList();
      taskUser.id = user.id;
      taskUser.name = user.name;
      taskUser.group = user.group;
      taskUser.atkNum = 0;
      taskUser.disNum = 0;
      taskUser.atkTeamNum = 0;
      taskUser.disTeamNum = 0;
      map.put(user.id, taskUser);
    }
    return map;
  }
}
