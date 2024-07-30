package com.atguigu.tingshu.user.login;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface TSLogin {
    LoginType value() default LoginType.WX_LOGIN;
}
