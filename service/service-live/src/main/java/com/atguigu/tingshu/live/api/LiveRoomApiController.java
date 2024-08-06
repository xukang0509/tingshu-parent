package com.atguigu.tingshu.live.api;

import com.atguigu.tingshu.common.login.AuthLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.live.service.LiveRoomService;
import com.atguigu.tingshu.model.live.LiveRoom;
import com.atguigu.tingshu.vo.live.LiveRoomVo;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("api/live/liveRoom")
@SuppressWarnings({"unchecked", "rawtypes"})
public class LiveRoomApiController {

    @Resource
    private LiveRoomService liveRoomService;

    // http://127.0.0.1:8500/api/live/liveRoom/getCurrentLive
    @AuthLogin
    @Operation(summary = "查看用户当前直播信息")
    @GetMapping("getCurrentLive")
    public Result<LiveRoom> getCurrentLive() {
        LiveRoom liveRoom = this.liveRoomService.getOne(Wrappers.lambdaQuery(LiveRoom.class)
                .eq(LiveRoom::getUserId, AuthContextHolder.getUserId())
                .gt(LiveRoom::getExpireTime, new Date()));
        return Result.ok(liveRoom);
    }

    // http://127.0.0.1:8500/api/live/liveRoom/saveLiveRoom
    @AuthLogin
    @Operation(summary = "保存用户当前直播信息")
    @PostMapping("saveLiveRoom")
    public Result<LiveRoom> saveLiveRoom(@RequestBody LiveRoomVo liveRoomVo) {
        return Result.ok(this.liveRoomService.saveLiveRoom(liveRoomVo));
    }

    // http://127.0.0.1:8500/api/live/liveRoom/getById/4
    @AuthLogin
    @Operation(summary = "根据id查询直播间")
    @GetMapping("getById/{liveRoomId}")
    public Result<LiveRoom> getLiveRoomById(@PathVariable Long liveRoomId) {
        return Result.ok(this.liveRoomService.getById(liveRoomId));
    }

    // http://127.0.0.1:8500/api/live/liveRoom/findLiveList
    @Operation(summary = "获取当前直播列表")
    @GetMapping("findLiveList")
    public Result<List<LiveRoom>> findLiveList() {
        return Result.ok(this.liveRoomService.list(Wrappers.lambdaQuery(LiveRoom.class)
                .gt(LiveRoom::getExpireTime, new Date())));
    }
}

