package com.atguigu.tingshu.account.client.impl;


import com.atguigu.tingshu.account.client.UserAccountFeignClient;
import com.atguigu.tingshu.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserAccountDegradeFeignClient implements UserAccountFeignClient {

    @Override
    public Result saveUserAccount(Long userId) {
        return Result.fail();
    }
}
