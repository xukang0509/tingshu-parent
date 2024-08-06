package com.atguigu.tingshu.live.api;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.live.service.LiveTagService;
import com.atguigu.tingshu.model.live.LiveTag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/live/liveTag")
@SuppressWarnings({"unchecked", "rawtypes"})
public class LiveTagApiController {

    @Autowired
    private LiveTagService liveTagService;

    // http://127.0.0.1:8500/api/live/liveTag/findAllLiveTag
    @Operation(summary = "获取直播标签")
    @GetMapping("findAllLiveTag")
    public Result<List<LiveTag>> findAllLiveTag() {
        return Result.ok(this.liveTagService.list());
    }
}

