package com.atguigu.tingshu.user.login.strategy;

import com.atguigu.tingshu.user.login.LoginForm;
import com.atguigu.tingshu.user.login.LoginStrategy;
import com.atguigu.tingshu.user.login.LoginType;
import com.atguigu.tingshu.user.login.TSLogin;

import java.util.Map;

@TSLogin(LoginType.PHONE_LOGIN)
public class PhoneLogin implements LoginStrategy {
    @Override
    public Map<String, Object> login(LoginForm form) {
        
        return null;
    }
}
