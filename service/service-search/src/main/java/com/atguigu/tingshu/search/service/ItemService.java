package com.atguigu.tingshu.search.service;

import com.alibaba.fastjson.JSONObject;

public interface ItemService {
    /**
     * 根据专辑id加载专辑详情
     *
     * @param albumId 专辑id
     * @return 专辑详情
     */
    JSONObject loadItem(Long albumId);
}
