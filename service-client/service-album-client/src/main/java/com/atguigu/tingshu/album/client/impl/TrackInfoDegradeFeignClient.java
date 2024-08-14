package com.atguigu.tingshu.album.client.impl;


import com.atguigu.tingshu.album.client.TrackInfoFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TrackInfoDegradeFeignClient implements TrackInfoFeignClient {
    @Override
    public Result<IPage<AlbumTrackListVo>> findAlbumTrackPage(Long albumId, Integer pageNum, Integer pageSize) {
        return null;
    }

    @Override
    public Result<TrackInfo> getTrackInfo(Long trackId) {
        return null;
    }

    @Override
    public Result<List<TrackInfo>> findTrackInfosByIdAndCount(Long trackId, Integer count) {
        return null;
    }
}
