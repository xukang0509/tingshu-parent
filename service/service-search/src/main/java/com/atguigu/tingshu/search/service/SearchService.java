package com.atguigu.tingshu.search.service;

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
}
