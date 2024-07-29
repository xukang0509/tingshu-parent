package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.model.user.UserInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface UserInfoService extends IService<UserInfo> {

    /**
     * 小程序微信授权登录
     *
     * @param code 临时登录凭证
     * @return token
     */
    Map<String, String> wxLogin(String code);
}
