package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.model.user.UserPaidTrack;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
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

    /**
     * 根据专辑id查询当前用户是否订购过该专辑
     *
     * @param albumId 专辑id
     * @param userId  用户id
     * @return true-当前用户订购过该专辑 false-当前用户未订购过该专辑
     */
    Boolean getPaidAlbumStat(Long albumId, Long userId);

    /**
     * 根据专辑id查询当前用户购买过该专辑下的声音列表
     *
     * @param albumId 专辑id
     * @param userId  用户id
     * @return 当前用户购买过该专辑下的声音列表
     */
    List<UserPaidTrack> getPaidTracksByAlbumIdAndUserId(Long albumId, Long userId);
}
