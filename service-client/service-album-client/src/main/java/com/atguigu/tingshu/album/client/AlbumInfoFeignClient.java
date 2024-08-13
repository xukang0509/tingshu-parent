package com.atguigu.tingshu.album.client;

import com.atguigu.tingshu.album.client.impl.AlbumInfoDegradeFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 * @author qy
 */
@FeignClient(value = "service-album", fallback = AlbumInfoDegradeFeignClient.class)
public interface AlbumInfoFeignClient {
    @PostMapping("api/album/albumInfo/findAllAlbumPage/{pageNum}/{pageSize}")
    Result<List<AlbumListVo>> findAllAlbumPage(
            @PathVariable("pageNum") Integer pageNum, @PathVariable("pageSize") Integer pageSize);

    @GetMapping("api/album/albumInfo/getAlbumInfo/{albumId}")
    Result<AlbumInfo> getAlbumInfo(@PathVariable("albumId") Long albumId);

    @PostMapping("api/album/albumInfo/findAlbumInfoAttributeValuesByAlbumInfoId/{albumInfoId}")
    Result<List<AlbumAttributeValue>> findAlbumInfoAttributeValueByAlbumInfoId(@PathVariable("albumInfoId") Long albumInfoId);

    @GetMapping("api/album/albumInfo/getAlbumState/{albumId}")
    Result<AlbumStatVo> getAlbumStatsByAlbumId(@PathVariable("albumId") Long albumId);

    @GetMapping("api/album/albumInfo/findLatelyUpdateAlbum/{startTime}/{endTime}")
    Result<List<Long>> findLatelyUpdateAlbum(@PathVariable("startTime") String startTime, @PathVariable("endTime") String endTime);

    @PostMapping("api/album/albumInfo/findAlbumStatVoList")
    Result<List<AlbumStatVo>> findAlbumStatVoList(@RequestBody List<Long> albumIds);
}