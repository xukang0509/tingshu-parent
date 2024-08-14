package com.atguigu.tingshu.album.client;

import com.atguigu.tingshu.album.client.impl.TrackInfoDegradeFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 * @author qy
 */
@FeignClient(value = "service-album", fallback = TrackInfoDegradeFeignClient.class)
public interface TrackInfoFeignClient {
    @GetMapping("api/album/trackInfo/findAlbumTrackPage/{albumId}/{pageNum}/{pageSize}")
    Result<IPage<AlbumTrackListVo>> findAlbumTrackPage(
            @PathVariable("albumId") Long albumId,
            @PathVariable("pageNum") Integer pageNum,
            @PathVariable("pageSize") Integer pageSize);

    @GetMapping("api/album/trackInfo/getTrackInfo/{trackId}")
    Result<TrackInfo> getTrackInfo(@PathVariable("trackId") Long trackId);

    @GetMapping("api/album/trackInfo/findTrackInfosByIdAndCount/{trackId}/{count}")
    Result<List<TrackInfo>> findTrackInfosByIdAndCount(
            @PathVariable("trackId") Long trackId,
            @PathVariable("count") Integer count);
}