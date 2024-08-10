package com.atguigu.tingshu.search.service.impl;

import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.album.client.CategoryFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.search.service.ItemService;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.atguigu.tingshu.vo.search.AlbumItemVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class ItemServiceImpl implements ItemService {

    @Resource
    private AlbumInfoFeignClient albumInfoFeignClient;

    @Resource
    private CategoryFeignClient categoryFeignClient;

    @Resource
    private UserInfoFeignClient userInfoFeignClient;

    @Resource
    private ExecutorService executorService;

    @Override
    public AlbumItemVo loadItem(Long albumId) {
        final AlbumItemVo data = new AlbumItemVo();

        // 根据专辑id查询专辑
        CompletableFuture<AlbumInfo> albumInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            Result<AlbumInfo> albumInfoRes = this.albumInfoFeignClient.getAlbumInfo(albumId);
            Assert.notNull(albumInfoRes, "查询专辑信息失败！");
            AlbumInfo albumInfo = albumInfoRes.getData();
            data.setAlbumInfo(albumInfo);
            return albumInfo;
        }, executorService);

        // 根据用户id查询主播信息
        CompletableFuture<Void> announcerCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            if (albumInfo == null) return;
            Result<UserInfoVo> userInfoRes = this.userInfoFeignClient.getUserInfoById(albumInfo.getUserId());
            Assert.notNull(userInfoRes, "获取用户信息失败！");
            UserInfoVo userInfoVo = userInfoRes.getData();
            data.setAnnouncer(userInfoVo);
        }, executorService);

        // 根据三级分类id查询分类信息
        CompletableFuture<Void> categoryViewCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            if (albumInfo == null) return;
            Result<BaseCategoryView> categoryViewRes = this.categoryFeignClient.findBaseCategoryViewByCategory3Id(albumInfo.getCategory3Id());
            Assert.notNull(categoryViewRes, "根据三级分类id获取分类信息失败！");
            BaseCategoryView baseCategoryView = categoryViewRes.getData();
            data.setBaseCategoryView(baseCategoryView);
        }, executorService);

        // 根据专辑id查询专辑统计信息s
        CompletableFuture<Void> albumStatVoCompletableFuture = CompletableFuture.runAsync(() -> {
            Result<AlbumStatVo> albumStatsRes = this.albumInfoFeignClient.getAlbumStatsByAlbumId(albumId);
            Assert.notNull(albumStatsRes, "根据专辑Id获取统计信息列表失败！");
            AlbumStatVo albumStatVo = albumStatsRes.getData();
            data.setAlbumStatVo(albumStatVo);
        }, executorService);

        CompletableFuture.allOf(
                albumInfoCompletableFuture,
                announcerCompletableFuture,
                albumStatVoCompletableFuture,
                categoryViewCompletableFuture
        ).join();
        return data;
    }
}
