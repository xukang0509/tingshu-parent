package com.atguigu.tingshu.common.cache;

import com.atguigu.tingshu.common.constant.RedisConstant;

import java.lang.annotation.*;

/**
 * 听书项目的缓存注解
 *
 * @author xk
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TingShuCache {
    /**
     * 缓存的前缀
     */
    String prefix() default RedisConstant.CACHE_INFO_PREFIX;

    /**
     * 分布式锁的前缀
     */
    String lockPrefix() default RedisConstant.CACHE_LOCK_PREFIX;

    /**
     * 设置缓存的有效时间，默认：30小时，单位：秒
     */
    long timeout() default RedisConstant.CACHE_INFO_TIMEOUT;

    /**
     * 防止缓存雪崩设置的随机值范围，默认：10分钟，单位：秒
     */
    long random() default RedisConstant.CACHE_RANDOM_TIMEOUT;

    /**
     * 为了防止缓存穿透，这里可以指定布隆过滤器在redis中的key
     */
    String bloomFilter() default "";
}
