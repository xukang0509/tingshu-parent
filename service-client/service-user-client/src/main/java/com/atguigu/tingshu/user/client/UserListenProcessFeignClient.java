package com.atguigu.tingshu.user.client;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.user.client.impl.UserListenProcessDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 * @author qy
 */
@FeignClient(value = "service-user", fallback = UserListenProcessDegradeFeignClient.class)
public interface UserListenProcessFeignClient {

    @GetMapping("api/user/userListenProcess/getTrackBreakSecond/{trackId}")
    Result<BigDecimal> getTrackBreakSecond(@PathVariable("trackId") Long trackId);
}