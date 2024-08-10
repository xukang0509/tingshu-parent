package com.atguigu.tingshu.common.cache;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 听书项目缓存处理的切面类
 *
 * @author xk
 */
@Aspect
@Component
public class TingShuCacheAspect {

    @Around("@annotation(tingShuCache)")
    public Object redisCache(ProceedingJoinPoint joinPoint, TingShuCache tingShuCache) {
        
        return null;
    }
}
