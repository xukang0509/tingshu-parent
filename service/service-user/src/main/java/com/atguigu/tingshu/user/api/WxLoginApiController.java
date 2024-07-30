package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.login.AuthLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserUpdateVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "微信授权登录接口")
@RestController
@RequestMapping("/api/user/wxLogin")
@Slf4j
public class WxLoginApiController {

    @Resource
    private UserInfoService userInfoService;

    // http://127.0.0.1:8500/api/user/wxLogin/wxLogin/0a32WRkl2n98Td4tqYnl2uKyDA32WRk2
    @Operation(summary = "小程序微信授权登录")
    @GetMapping("wxLogin/{code}")
    public Result<Map<String, Object>> wxLogin(@PathVariable String code) {
        return Result.ok(this.userInfoService.login(code));
    }

    // http://127.0.0.1:8500/api/user/wxLogin/getUserInfo
    @AuthLogin
    @Operation(summary = "获取用户信息")
    @GetMapping("getUserInfo")
    public Result<UserInfoVo> getUserInfo() {
        UserInfo userInfo = this.userInfoService.getById(AuthContextHolder.getUserId());
        UserInfoVo userInfoVo = new UserInfoVo();
        BeanUtils.copyProperties(userInfo, userInfoVo);
        return Result.ok(userInfoVo);
    }

    // http://127.0.0.1:8500/api/user/wxLogin/updateUser
    @AuthLogin
    @Operation(summary = "更新用户信息")
    @PostMapping("updateUser")
    public Result<Void> updateUserInfo(@RequestBody UserUpdateVo userUpdateVo) {
        UserInfo userInfo = new UserInfo();
        BeanUtils.copyProperties(userUpdateVo, userInfo);
        userInfo.setId(AuthContextHolder.getUserId());
        this.userInfoService.updateById(userInfo);
        return Result.ok();
    }
}
