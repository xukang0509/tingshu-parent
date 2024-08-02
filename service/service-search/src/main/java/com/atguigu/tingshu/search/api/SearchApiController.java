package com.atguigu.tingshu.search.api;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}

