package com.atguigu.tingshu.album.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.atguigu.tingshu.vo.album.TrackStatVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface TrackInfoService extends IService<TrackInfo> {

    /**
     * 新增声音
     *
     * @param trackInfoVo 声音信息
     */
    void saveTrackInfo(TrackInfoVo trackInfoVo);

    /**
     * 分页查询声音列表
     *
     * @param pageNum        当前页码
     * @param pageSize       每页显示条数
     * @param trackInfoQuery 查询条件
     * @return 声音列表
     */
    Page<TrackListVo> findUserTrackPage(Integer pageNum, Integer pageSize, TrackInfoQuery trackInfoQuery);

    /**
     * 更新声音
     *
     * @param trackInfoVo 声音信息
     * @param trackId     声音主键ID
     */
    void updateTrackInfoById(TrackInfoVo trackInfoVo, Long trackId);

    /**
     * 删除声音
     *
     * @param trackId 声音主键Id
     */
    void removeTrackInfo(Long trackId);

    /**
     * 根据专辑id分页查询声音
     *
     * @param albumId 专辑id
     * @param page    分页
     * @return 用户专辑声音列表信息
     */
    IPage<AlbumTrackListVo> findAlbumTrackPage(Long albumId, Page<AlbumTrackListVo> page);

    /**
     * 查询声音统计信息
     *
     * @param trackId 声音id
     * @return 声音统计信息
     */
    TrackStatVo getTrackStatVo(Long trackId);

    /**
     * 获取声音播放凭证
     *
     * @param trackId 声音id
     * @return 播放凭证
     */
    JSONObject getPlayToken(Long trackId);
}
