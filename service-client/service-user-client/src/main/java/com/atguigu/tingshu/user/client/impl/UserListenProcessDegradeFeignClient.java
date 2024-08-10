package com.atguigu.tingshu.user.client.impl;


import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.user.client.UserListenProcessFeignClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class UserListenProcessDegradeFeignClient implements UserListenProcessFeignClient {

    @Override
    public Result<BigDecimal> getTrackBreakSecond(Long trackId) {
        return null;
    }
}
