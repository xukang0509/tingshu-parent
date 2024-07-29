package com.atguigu.tingshu.common.login;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AuthLogin {
    /**
     * 是否必须要登录
     */
    boolean required() default true;
}