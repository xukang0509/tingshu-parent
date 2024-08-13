package com.atguigu.tingshu.user.client.impl;


import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.user.UserPaidTrack;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserInfoDegradeFeignClient implements UserInfoFeignClient {

    @Override
    public Result<UserInfoVo> getUserInfoById(Long id) {
        return null;
    }

    @Override
    public Result<Boolean> getPaidAlbumStat(Long albumId) {
        return null;
    }

    @Override
    public Result<List<UserPaidTrack>> getPaidTracksByAlbumIdAndUserId(Long albumId) {
        return null;
    }

    @Override
    public Result updateExpiredVipStatus() {
        return null;
    }
}
