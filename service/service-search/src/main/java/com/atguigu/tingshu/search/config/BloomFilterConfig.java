package com.atguigu.tingshu.search.config;

import com.atguigu.tingshu.common.constant.RedisConstant;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Configuration;

/**
 * @author xk
 * @since 2024-08-12 11:34
 */
@Configuration
public class BloomFilterConfig {
    @Resource
    private RedissonClient redissonClient;

    @PostConstruct
    public void init() {
        RBloomFilter<Object> bloomFilter = this.redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
        // 期望插入数据数量：1百万，精确度：97%
        bloomFilter.tryInit(100_0000, 0.03);
    }
}