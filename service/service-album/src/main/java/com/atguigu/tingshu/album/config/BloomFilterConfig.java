package com.atguigu.tingshu.album.config;

import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

/**
 * @author xk
 * @since 2024-08-12 11:34
 */
@Configuration
public class BloomFilterConfig {
    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private AlbumInfoService albumInfoService;

    @PostConstruct
    public void init() {
        this.redisTemplate.delete(RedisConstant.ALBUM_BLOOM_FILTER);
        RBloomFilter<String> bloomFilter = this.redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
        // 期望插入数据数量：1百万，精确度：97%
        bloomFilter.tryInit(100_0000, 0.03);
        // 将数据库中的所有专辑的id存入布隆过滤器
        List<Long> albumIds = this.albumInfoService.list(Wrappers.lambdaQuery(AlbumInfo.class)
                        .eq(AlbumInfo::getIsDeleted, 0)
                        .select(AlbumInfo::getId))
                .stream().map(AlbumInfo::getId).toList();
        for (Long albumId : albumIds) {
            bloomFilter.add(RedisConstant.ALBUM_INFO_PREFIX + albumId);
        }
    }
}
