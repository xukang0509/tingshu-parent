package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.atguigu.tingshu.vo.album.VodFileUploadVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "声音管理")
@RestController
@RequestMapping("api/album/trackInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class TrackInfoApiController {

    @Resource
    private TrackInfoService trackInfoService;

    @Resource
    private VodService vodService;

    // http://localhost:8500/api/album/trackInfo/uploadTrack
    @Operation(summary = "上传声音")
    @PostMapping("uploadTrack")
    public Result<VodFileUploadVo> uploadTrack(MultipartFile file) {
        return Result.ok(this.vodService.uploadTrack(file));
    }

    // http://localhost:8500/api/album/trackInfo/saveTrackInfo
    @Operation(summary = "新增声音")
    @PostMapping("saveTrackInfo")
    public Result<Void> saveTrackInfo(@RequestBody @Validated TrackInfoVo trackInfoVo) {
        this.trackInfoService.saveTrackInfo(trackInfoVo);
        return Result.ok();
    }

    // http://localhost:8500/api/album/trackInfo/findUserTrackPage/1/10
    @Operation(summary = "分页查询声音列表")
    @PostMapping("findUserTrackPage/{pageNum}/{pageSize}")
    public Result<Page<TrackListVo>> findUserTrackPage(
            @Parameter(name = "pageNum", description = "当前页码", required = true)
            @PathVariable Integer pageNum,
            @Parameter(name = "pageSize", description = "每页显示条数", required = true)
            @PathVariable Integer pageSize,
            @Parameter(name = "trackInfoQuery", description = "查询条件", required = false)
            @RequestBody TrackInfoQuery trackInfoQuery) {
        Page<TrackListVo> page = this.trackInfoService.findUserTrackPage(pageNum, pageSize, trackInfoQuery);
        return Result.ok(page);
    }

    // http://localhost:8500/api/album/trackInfo/getTrackInfo/51942
    @Operation(summary = "获取声音详情：数据回显")
    @GetMapping("getTrackInfo/{trackId}")
    public Result<TrackInfo> getTrackInfo(@PathVariable Long trackId) {
        return Result.ok(this.trackInfoService.getById(trackId));
    }

    // http://localhost:8500/api/album/trackInfo/updateTrackInfo/51933
    @Operation(summary = "更新声音")
    @PutMapping("updateTrackInfo/{trackId}")
    public Result<Void> updateTrackInfo(@RequestBody @Validated TrackInfoVo trackInfoVo, @PathVariable Long trackId) {
        this.trackInfoService.updateTrackInfoById(trackInfoVo, trackId);
        return Result.ok();
    }

    // http://localhost:8500/api/album/trackInfo/removeTrackInfo/51942
    @Operation(summary = "删除声音")
    @DeleteMapping("removeTrackInfo/{trackId}")
    public Result<Void> removeTrackInfo(@PathVariable Long trackId) {
        this.trackInfoService.removeTrackInfo(trackId);
        return Result.ok();
    }
}

