package com.atguigu.tingshu.search.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.vo.search.AlbumInfoIndexVo;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "搜索专辑管理")
@RestController
@RequestMapping("api/search/albumInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class SearchApiController {

    @Autowired
    private SearchService searchService;

    @Operation(summary = "商品上架")
    @GetMapping("upper/album/{albumId}")
    public Result upperAlbum(@PathVariable("albumId") Long albumId) {
        this.searchService.upperAlbum(albumId);
        return Result.ok();
    }

    @Operation(summary = "商品下架")
    @GetMapping("down/album/{albumId}")
    public Result downAlbum(@PathVariable("albumId") Long albumId) {
        this.searchService.downAlbum(albumId);
        return Result.ok();
    }

    // http://127.0.0.1:8500/api/search/albumInfo/channel/5
    @Operation(summary = "查询一级分类下置顶三级分类的热门频道数据")
    @GetMapping("channel/{category1Id}")
    public Result<List<JSONObject>> channel(@PathVariable Long category1Id) {
        return Result.ok(this.searchService.channel(category1Id));
    }

    // http://127.0.0.1:8500/api/search/albumInfo
    @Operation(summary = "实现条件搜索专辑信息功能")
    @PostMapping
    public Result<AlbumSearchResponseVo> searchAlbumInfoIndex(@RequestBody AlbumIndexQuery albumIndexQuery) {
        return Result.ok(this.searchService.searchAlbumInfoIndex(albumIndexQuery));
    }

    // http://127.0.0.1:8500/api/search/albumInfo/completeSuggest/%E6%89%93
    @Operation(summary = "关键字补全提示")
    @GetMapping("completeSuggest/{keyWord}")
    public Result<List<String>> completeSuggest(@PathVariable String keyWord) {
        return Result.ok(this.searchService.completeSuggest(keyWord));
    }

    // http://127.0.0.1:8500/api/search/albumInfo/findRankingList/1/commentStatNum
    @Operation(summary = "获取排行榜列表")
    @GetMapping("findRankingList/{category1Id}/{orderField}")
    public Result<List<AlbumInfoIndexVo>> findRankingList(@PathVariable Long category1Id, @PathVariable String orderField) {
        return Result.ok(this.searchService.findRankingList(category1Id, orderField));
    }

    @Operation(summary = "更新最近的专辑统计信息到es")
    @GetMapping("updateLatelyAlbumStat")
    public Result updateLatelyAlbumStat() {
        this.searchService.updateLatelyAlbumStat();
        return Result.ok();
    }
}

