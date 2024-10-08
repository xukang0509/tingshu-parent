package com.atguigu.tingshu.album.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface BaseCategoryService extends IService<BaseCategory1> {

    /**
     * 获取专辑分类数据
     *
     * @return 专辑分类数据集合，JSONObject(Map)格式
     */
    List<JSONObject> getBaseCategoryList();

    /**
     * 根据一级分类id获取专辑分类属性标签
     *
     * @param category1Id 一级分类id
     * @return 专辑属性
     */
    List<BaseAttribute> findAttributeByCategory1Id(Long category1Id);

    /**
     * 根据三级分类id查询分类
     *
     * @param category3Id 三级分类id
     * @return 分类视图
     */
    BaseCategoryView findBaseCategoryViewByCategory3Id(Long category3Id);


    /**
     * 根据一级分类Id查询置顶频道页的三级分类
     *
     * @param category1Id 一级分类Id
     * @return 三级分类列表
     */
    List<BaseCategory3> findTopBaseCategory3ByCategory1Id(Long category1Id);

    /**
     * 根据一级分类id获取全部分类信息
     *
     * @param category1Id 一级分类Id
     * @return 专辑分类数据，JSONObject(Map)格式
     */
    JSONObject getBaseCategoryList(Long category1Id);
}
