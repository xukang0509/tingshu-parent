package com.atguigu.tingshu.search.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.vo.search.AlbumInfoIndexVo;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;

import java.util.List;

public interface SearchService {

    /**
     * 上架专辑
     *
     * @param albumId 专辑id
     */
    void upperAlbum(Long albumId);

    /**
     * 下架专辑
     *
     * @param albumId 专辑id
     */
    void downAlbum(Long albumId);

    /**
     * 查询一级分类下置顶三级分类的热门频道数据
     *
     * @param category1Id 一级分类id
     * @return 结果集
     */
    List<JSONObject> channel(Long category1Id);

    /**
     * 实现条件搜索专辑信息功能
     *
     * @param albumIndexQuery 条件信息
     * @return 结果集
     */
    AlbumSearchResponseVo searchAlbumInfoIndex(AlbumIndexQuery albumIndexQuery);


    /**
     * 关键字补全提示
     *
     * @param keyWord 关键字
     * @return 补全提示集合
     */
    List<String> completeSuggest(String keyWord);

    /**
     * 获取排行榜列表
     *
     * @param category1Id 一级分类id
     * @param orderField  排列类型[hotScore、playStatNum、subscribeStatNum、buyStatNum、commentStatNum]
     * @return
     */
    List<AlbumInfoIndexVo> findRankingList(Long category1Id, String orderField);

    /**
     * 更新最近的专辑统计信息到es
     */
    void updateLatelyAlbumStat();
}
