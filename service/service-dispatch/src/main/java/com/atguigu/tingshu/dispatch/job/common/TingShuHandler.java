package com.atguigu.tingshu.dispatch.job.common;

import java.lang.annotation.*;

/**
 * @author xk
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TingShuHandler {
    /**
     * 任务的描述
     *
     * @return
     */
    String describe() default "";
}
