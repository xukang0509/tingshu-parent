package com.atguigu.tingshu.common.cache;

import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 听书项目缓存处理的切面类
 *
 * @author xk
 */
@Aspect
@Component
public class TingShuCacheAspect {
    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Around("@annotation(tingShuCache)")
    public Object redisCache(ProceedingJoinPoint joinPoint, TingShuCache tingShuCache) {
        // 前缀
        String prefix = tingShuCache.prefix();
        // 获取方法的参数
        Object[] args = joinPoint.getArgs();
        String param = StringUtils.join(args, ":");
        // 组装key
        String key = prefix + param;

        // 先查询缓存，如果命中则直接返回
        Object res = this.redisTemplate.opsForValue().get(key);
        if (res != null) return res;

        if (StringUtils.isNotBlank(tingShuCache.bloomFilter())) {
            // 缓存中没有该数据，则检查布隆过滤器中是否有该数据，如果没有则直接返回null (解决缓存穿透问题)
            RBloomFilter<String> bloomFilter = this.redissonClient.getBloomFilter(tingShuCache.bloomFilter());
            if (!bloomFilter.contains(key)) return null;
        }

        // 使用分布式锁，解决缓存击穿问题
        RLock lock = this.redissonClient.getLock(tingShuCache.lockPrefix() + param);
        // 加锁
        lock.lock();
        try {
            // 在等待获取锁的过程中，可能有其他请求提前获取了锁，并把数据放入了缓存。
            // 所以为了提高性能再次查询缓存，如果命中则直接返回
            res = this.redisTemplate.opsForValue().get(key);
            if (res != null) return res;

            // 执行目标方法
            res = joinPoint.proceed();
            // 放入缓存，随机化过期时间，解决缓存雪崩问题
            this.redisTemplate.opsForValue().set(key, res,
                    tingShuCache.timeout() + new Random().nextLong(tingShuCache.random()),
                    TimeUnit.SECONDS);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            // 释放锁
            lock.unlock();
        }
        return res;
    }
}
