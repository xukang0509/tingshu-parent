package com.atguigu.tingshu.common.cache;

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
    String prefix() default "";

    /**
     * 设置缓存的有效时间，单位：秒
     */
    long timeout() default 10 * 60L;

    /**
     * 防止缓存雪崩设置的随机值范围，单位：秒
     */
    long random() default 5 * 60L;

    /**
     * 为了防止缓存穿透，这里可以指定布隆过滤器在redis中的key
     */
    String bloomFilter() default "";
}
