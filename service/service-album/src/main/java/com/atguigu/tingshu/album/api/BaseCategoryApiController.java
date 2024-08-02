package com.atguigu.tingshu.album.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Tag(name = "分类管理")
@RestController
@RequestMapping(value = "/api/album/category")
@SuppressWarnings({"unchecked", "rawtypes"})
public class BaseCategoryApiController {

    @Autowired
    private BaseCategoryService baseCategoryService;

    // http://localhost:8500/api/album/category/getBaseCategoryList
    @Operation(summary = "获取专辑分类数据")
    @GetMapping("getBaseCategoryList")
    public Result<List<JSONObject>> getBaseCategoryList() {
        return Result.ok(this.baseCategoryService.getBaseCategoryList());
    }

    // http://localhost:8500/api/album/category/findAttribute/3
    @Operation(summary = "根据一级分类id获取专辑分类属性标签")
    @GetMapping("findAttribute/{category1Id}")
    public Result<List<BaseAttribute>> findAttribute(@PathVariable Long category1Id) {
        return Result.ok(this.baseCategoryService.findAttributeByCategory1Id(category1Id));
    }

    @Operation(summary = "根据三级分类id查询分类")
    @GetMapping("findBaseCategoryViewByCategory3Id/{category3Id}")
    public Result<BaseCategoryView> findBaseCategoryViewByCategory3Id(@PathVariable("category3Id") Long category3Id) {
        return Result.ok(this.baseCategoryService.findBaseCategoryViewByCategory3Id(category3Id));
    }
}

