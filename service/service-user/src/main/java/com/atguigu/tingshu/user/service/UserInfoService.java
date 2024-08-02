package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface UserInfoService extends IService<UserInfo> {

    /**
     * 小程序微信授权登录
     *
     * @param code 临时登录凭证
     * @return token
     */
    Map<String, Object> login(String code);

    /**
     * 根据用户id查询用户
     *
     * @param id 用户id
     * @return 用户vo
     */
    UserInfoVo getUserInfoById(Long id);
}
