package com.atguigu.tingshu.user.login;

import java.util.Map;

public interface LoginStrategy {
    Map<String, Object> login(LoginForm form);
}
