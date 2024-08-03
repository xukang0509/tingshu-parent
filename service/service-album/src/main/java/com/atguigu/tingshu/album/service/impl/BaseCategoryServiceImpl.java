package com.atguigu.tingshu.album.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.mapper.*;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.model.album.*;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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
        catagory1Map.forEach((category1Id, category2List) -> {
            // 每一个k-v转化为一级分类的json对象
            JSONObject category1Json = new JSONObject();
            // 将一级分类json对象放入分类集合
            category1JsonList.add(category1Json);
            // 设置一级分类id和一级分类名称
            category1Json.put("categoryId", category1Id);
            category1Json.put("categoryName", category2List.get(0).getCategory1Name());
            // 根据二级分类id对同一个一级分类下的数据进行分组，获取二级分类数据Map<二级分类id，同一个二级分类id下的数据集合>
            Map<Long, List<BaseCategoryView>> category2Map = category2List.stream()
                    .collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            // 设置一级分类下的二级分类
            List<JSONObject> category2JsonList = new ArrayList<>();
            category1Json.put("categoryChild", category2JsonList);
            category2Map.forEach((category2Id, category3List) -> {
                // 每一个k-v转化为二级分类的json对象
                JSONObject category2Json = new JSONObject();
                category2JsonList.add(category2Json);
                category2Json.put("categoryId", category2Id);
                category2Json.put("categoryName", category3List.get(0).getCategory2Name());
                // 设置二级分类下的三级分类
                List<JSONObject> category3JsonList = new ArrayList<>();
                category2Json.put("categoryChild", category3JsonList);
                category3List.forEach(baseCategoryView -> {
                    // 每一个k-v转化为三级分类的json对象
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

    @Override
    public BaseCategoryView findBaseCategoryViewByCategory3Id(Long category3Id) {
        return this.baseCategoryViewMapper.selectOne(Wrappers.lambdaQuery(BaseCategoryView.class)
                .eq(BaseCategoryView::getCategory3Id, category3Id)
                .last("limit 1"));
    }

    @Override
    public List<BaseCategory3> findTopBaseCategory3ByCategory1Id(Long category1Id) {
        List<BaseCategory2> baseCategory2List = this.baseCategory2Mapper.selectList(Wrappers.lambdaQuery(BaseCategory2.class)
                .eq(BaseCategory2::getCategory1Id, category1Id)
                .select(BaseCategory2::getId));
        if (CollectionUtils.isEmpty(baseCategory2List)) return null;
        List<Long> category2Ids = baseCategory2List.stream().map(BaseCategory2::getId).toList();
        return this.baseCategory3Mapper.selectList(Wrappers.lambdaQuery(BaseCategory3.class)
                .in(BaseCategory3::getCategory2Id, category2Ids)
                .eq(BaseCategory3::getIsTop, 1)
                .last("limit 7"));
    }

    @Override
    public JSONObject getBaseCategoryList(Long category1Id) {
        List<BaseCategoryView> categoryViews = this.baseCategoryViewMapper.selectList(Wrappers.lambdaQuery(BaseCategoryView.class)
                .eq(BaseCategoryView::getCategory1Id, category1Id));
        if (CollectionUtils.isEmpty(categoryViews)) return null;
        JSONObject category1Json = new JSONObject();
        category1Json.put("categoryId", category1Id);
        category1Json.put("categoryName", categoryViews.get(0).getCategory1Name());
        // 根据二级分类id对同一个一级分类下的数据进行分组，获取二级分类数据Map<二级分类id，同一个二级分类id下的数据集合>
        Map<Long, List<BaseCategoryView>> category2IdToItems = categoryViews.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
        List<JSONObject> category2Children = new ArrayList<>();
        category1Json.put("categoryChild", category2Children);
        category2IdToItems.forEach((category2Id, categoryView3List) -> {
            JSONObject category2Json = new JSONObject();
            category2Json.put("categoryId", category2Id);
            category2Json.put("categoryName", categoryView3List.get(0).getCategory2Name());
            List<JSONObject> category3Children = new ArrayList<>();
            category2Json.put("categoryChild", category3Children);
            categoryView3List.forEach(categoryView3 -> {
                JSONObject category3Json = new JSONObject();
                category3Json.put("categoryId", categoryView3.getCategory3Id());
                category3Json.put("categoryName", categoryView3.getCategory3Name());
                category3Children.add(category3Json);
            });
            category2Children.add(category2Json);
        });
        return category1Json;
    }
}
