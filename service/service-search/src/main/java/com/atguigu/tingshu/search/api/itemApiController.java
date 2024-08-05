package com.atguigu.tingshu.search.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.common.login.AuthLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.search.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "专辑详情管理")
@RestController
@RequestMapping("api/search/albumInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class itemApiController {

    @Resource
    private ItemService itemService;

    // http://127.0.0.1:8500/api/search/albumInfo/1
    @AuthLogin(required = false)
    @Operation(summary = "加载专辑详情")
    @GetMapping("{albumId}")
    public Result<JSONObject> loadItem(@PathVariable("albumId") Long albumId) {
        return Result.ok(this.itemService.loadItem(albumId));
    }
}

