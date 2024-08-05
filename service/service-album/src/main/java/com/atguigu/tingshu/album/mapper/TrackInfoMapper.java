package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TrackInfoMapper extends BaseMapper<TrackInfo> {
    Page<TrackListVo> findUserTrackPage(Page<TrackListVo> page, @Param("query") TrackInfoQuery trackInfoQuery);

    IPage<AlbumTrackListVo> selectAlbumTrackPageByAlbumId(@Param("albumId") Long albumId, Page<AlbumTrackListVo> page);
}
