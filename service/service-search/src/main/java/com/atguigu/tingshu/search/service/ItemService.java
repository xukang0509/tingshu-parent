package com.atguigu.tingshu.search.service;

import com.atguigu.tingshu.vo.search.AlbumItemVo;

public interface ItemService {
    /**
     * 根据专辑id加载专辑详情
     *
     * @param albumId 专辑id
     * @return 专辑详情
     */
    AlbumItemVo loadItem(Long albumId);
}
