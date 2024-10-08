package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.login.AuthLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "专辑管理")
@RestController
@RequestMapping("api/album/albumInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class AlbumInfoApiController {
    @Resource
    private AlbumInfoService albumInfoService;

    // http://localhost:8500/api/album/albumInfo/saveAlbumInfo
    @AuthLogin
    @Operation(summary = "新增专辑")
    @PostMapping("saveAlbumInfo")
    public Result<Void> saveAlbumInfo(@RequestBody @Validated AlbumInfoVo albumInfoVo) {
        this.albumInfoService.saveAlbumInfo(albumInfoVo);
        return Result.ok();
    }

    // http://localhost:8500/api/album/albumInfo/findUserAlbumPage/1/10
    @AuthLogin
    @Operation(summary = "分页条件查询专辑")
    @PostMapping("findUserAlbumPage/{pageNum}/{pageSize}")
    public Result<Page<AlbumListVo>> findUserAlbumPage(
            @Parameter(name = "pageNum", description = "当前页码", required = true)
            @PathVariable Integer pageNum,
            @Parameter(name = "pageSize", description = "每页显示条数", required = true)
            @PathVariable Integer pageSize,
            @Parameter(name = "albumInfoQuery", description = "查询条件", required = false)
            @RequestBody AlbumInfoQuery albumInfoQuery) {
        Page<AlbumListVo> albumListVoPage = this.albumInfoService.findUserAlbumPage(pageNum, pageSize, albumInfoQuery);
        return Result.ok(albumListVoPage);
    }

    // http://localhost:8500/api/album/albumInfo/removeAlbumInfo/1
    @AuthLogin
    @Operation(summary = "删除专辑")
    @DeleteMapping("removeAlbumInfo/{albumId}")
    public Result<Void> removeAlbumInfo(@PathVariable Long albumId) {
        this.albumInfoService.removeAlbumInfoById(albumId);
        return Result.ok();
    }

    // http://localhost:8500/api/album/albumInfo/getAlbumInfo/1
    @Operation(summary = "获取专辑详情：数据回显")
    @GetMapping("getAlbumInfo/{albumId}")
    public Result<AlbumInfo> getAlbumInfo(@PathVariable Long albumId) {
        return Result.ok(this.albumInfoService.getAlbumInfoById(albumId));
    }

    // http://localhost:8500/api/album/albumInfo/updateAlbumInfo/1
    @AuthLogin
    @Operation(summary = "更新专辑")
    @PutMapping("updateAlbumInfo/{albumId}")
    public Result<Void> updateAlbumInfo(@RequestBody @Validated AlbumInfoVo albumInfoVo, @PathVariable Long albumId) {
        this.albumInfoService.updateAlbumInfo(albumInfoVo, albumId);
        return Result.ok();
    }

    // http://localhost:8500/api/album/albumInfo/findUserAllAlbumList
    @AuthLogin
    @Operation(summary = "查看当前用户的专辑列表")
    @GetMapping("findUserAllAlbumList")
    public Result<List<AlbumInfo>> findUserAllAlbumList() {
        return Result.ok(this.albumInfoService.findUserAllAlbumList());
    }


    @Operation(summary = "分页查询所有专辑信息")
    @PostMapping("findAllAlbumPage/{pageNum}/{pageSize}")
    public Result<List<AlbumListVo>> findAllAlbumPage(
            @Parameter(name = "pageNum", description = "当前页码", required = true)
            @PathVariable Integer pageNum,
            @Parameter(name = "pageSize", description = "每页显示条数", required = true)
            @PathVariable Integer pageSize) {
        Page<AlbumListVo> albumListVoPage = this.albumInfoService.findAllAlbumPage(pageNum, pageSize);
        return Result.ok(albumListVoPage.getRecords());
    }

    @Operation(summary = "根据专辑id查询专辑属性信息")
    @PostMapping("findAlbumInfoAttributeValuesByAlbumInfoId/{albumInfoId}")
    public Result<List<AlbumAttributeValue>> findAlbumInfoAttributeValueByAlbumInfoId(@PathVariable("albumInfoId") Long albumInfoId) {
        return Result.ok(this.albumInfoService.findAlbumInfoAttributeValueByAlbumInfoId(albumInfoId));
    }

    @Operation(summary = "根据专辑id获取专辑统计信息")
    @GetMapping("getAlbumState/{albumId}")
    public Result<AlbumStatVo> getAlbumStatsByAlbumId(@PathVariable("albumId") Long albumId) {
        return Result.ok(this.albumInfoService.getAlbumStatsByAlbumId(albumId));
    }

    // http://localhost:8500/api/album/albumInfo/findLatelyUpdateAlbum/2024-07-10/2024-08-14
    @Operation(summary = "查询统计信息最近更新过的专辑id列表")
    @GetMapping("findLatelyUpdateAlbum/{startTime}/{endTime}")
    public Result<List<Long>> findLatelyUpdateAlbum(@PathVariable("startTime") String startTime, @PathVariable("endTime") String endTime) {
        List<Long> albumIds = this.albumInfoService.findLatelyUpdateAlbum(startTime, endTime);
        return Result.ok(albumIds);
    }

    // http://localhost:8500/api/album/albumInfo/findAlbumStatVoList
    @Operation(summary = "根据专辑id列表查询统计信息")
    @PostMapping("findAlbumStatVoList")
    public Result<List<AlbumStatVo>> findAlbumStatVoList(@RequestBody List<Long> albumIds) {
        List<AlbumStatVo> albumStatVos = this.albumInfoService.findAlbumStatVoList(albumIds);
        return Result.ok(albumStatVos);
    }
}

