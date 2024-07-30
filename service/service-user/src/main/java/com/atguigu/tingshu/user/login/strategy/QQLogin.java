package com.atguigu.tingshu.user.login.strategy;

import com.atguigu.tingshu.user.login.LoginForm;
import com.atguigu.tingshu.user.login.LoginStrategy;
import com.atguigu.tingshu.user.login.LoginType;
import com.atguigu.tingshu.user.login.TSLogin;

import java.util.Map;

@TSLogin(LoginType.QQ_LOGIN)
public class QQLogin implements LoginStrategy {
    @Override
    public Map<String, Object> login(LoginForm form) {
        
        return null;
    }
}
