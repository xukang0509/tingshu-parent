package com.atguigu.tingshu.album.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.mapper.*;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class BaseCategoryServiceImpl extends ServiceImpl<BaseCategory1Mapper, BaseCategory1> implements BaseCategoryService {

    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;

    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;

    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;

    @Resource
    private BaseCategoryViewMapper baseCategoryViewMapper;

    @Resource
    private BaseAttributeMapper baseAttributeMapper;

    @Override
    public List<JSONObject> getBaseCategoryList() {
        // 查询所有分类
        List<BaseCategoryView> baseCategoryViews = baseCategoryViewMapper.selectList(null);
        if (CollectionUtils.isEmpty(baseCategoryViews)) return null;
        // 根据一级分类id对分类进行分组，获取一级分类数据<一级分类id, 同一个一级分类id下的数据集合>
        Map<Long, List<BaseCategoryView>> catagory1Map = baseCategoryViews.stream()
                .collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        List<JSONObject> category1JsonList = new ArrayList<>();
        catagory1Map.forEach((category1, category2List) -> {
            JSONObject category1Json = new JSONObject();
            category1JsonList.add(category1Json);
            category1Json.put("categoryId", category1);
            category1Json.put("categoryName", category2List.get(0).getCategory1Name());
            Map<Long, List<BaseCategoryView>> category2Map = category2List.stream()
                    .collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            List<JSONObject> category2JsonList = new ArrayList<>();
            category1Json.put("categoryChild", category2JsonList);
            category2Map.forEach((category2, category3List) -> {
                JSONObject category2Json = new JSONObject();
                category2JsonList.add(category2Json);
                category2Json.put("categoryId", category2);
                category2Json.put("categoryName", category3List.get(0).getCategory2Name());
                List<JSONObject> category3JsonList = new ArrayList<>();
                category2Json.put("categoryChild", category3JsonList);
                category3List.forEach(baseCategoryView -> {
                    JSONObject category3Json = new JSONObject();
                    category3JsonList.add(category3Json);
                    category3Json.put("categoryId", baseCategoryView.getCategory3Id());
                    category3Json.put("categoryName", baseCategoryView.getCategory3Name());
                });
            });
        });
        return category1JsonList;
    }

    @Override
    public List<BaseAttribute> findAttributeByCategory1Id(Long category1Id) {
        return this.baseAttributeMapper.selectAttributeByCategory1Id(category1Id);
    }
}
