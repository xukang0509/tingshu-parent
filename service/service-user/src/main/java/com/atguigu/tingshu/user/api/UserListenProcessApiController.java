package com.atguigu.tingshu.user.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.common.login.AuthLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.user.service.UserListenProcessService;
import com.atguigu.tingshu.vo.user.UserListenProcessVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "用户声音播放进度管理接口")
@RestController
@RequestMapping("api/user/userListenProcess")
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserListenProcessApiController {

    @Resource
    private UserListenProcessService userListenProcessService;

    // http://127.0.0.1:8500/api/user/userListenProcess/getTrackBreakSecond/31846
    @AuthLogin(required = false)
    @Operation(summary = "根据声音id获取播放进度")
    @GetMapping("getTrackBreakSecond/{trackId}")
    public Result<BigDecimal> getTrackBreakSecond(@PathVariable("trackId") Long trackId) {
        return Result.ok(this.userListenProcessService.getTrackBreakSecond(trackId));
    }

    // http://127.0.0.1:8500/api/user/userListenProcess/updateListenProcess
    @AuthLogin(required = false)
    @Operation(summary = "更新播放进度")
    @PostMapping("updateListenProcess")
    public Result updateListenProcess(@RequestBody UserListenProcessVo userListenProcessVo) {
        this.userListenProcessService.updateListenProcess(userListenProcessVo);
        return Result.ok();
    }

    // http://127.0.0.1:8500/api/user/userListenProcess/getLatelyTrack
    @AuthLogin
    @Operation(summary = "获取最近一次播放声音")
    @GetMapping("getLatelyTrack")
    public Result<JSONObject> getLatelyTrack() {
        return Result.ok(this.userListenProcessService.getLatelyTrack());
    }
}

