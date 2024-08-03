package com.atguigu.tingshu.album.client.impl;


import com.atguigu.tingshu.album.client.CategoryFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CategoryDegradeFeignClient implements CategoryFeignClient {

    @Override
    public Result<BaseCategoryView> findBaseCategoryViewByCategory3Id(Long category3Id) {
        return null;
    }

    @Override
    public Result<List<BaseCategory3>> findTopBaseCategory3ByCategory1Id(Long category1Id) {
        return null;
    }
}
