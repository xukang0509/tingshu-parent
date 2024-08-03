package com.atguigu.tingshu.album.client;

import com.atguigu.tingshu.album.client.impl.CategoryDegradeFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 * @author qy
 */
@FeignClient(value = "service-album", fallback = CategoryDegradeFeignClient.class)
public interface CategoryFeignClient {

    @GetMapping("api/album/category/findBaseCategoryViewByCategory3Id/{category3Id}")
    Result<BaseCategoryView> findBaseCategoryViewByCategory3Id(
            @PathVariable("category3Id") Long category3Id);

    @GetMapping("api/album/category/findTopBaseCategory3/{category1Id}")
    Result<List<BaseCategory3>> findTopBaseCategory3ByCategory1Id(
            @PathVariable("category1Id") Long category1Id);
}