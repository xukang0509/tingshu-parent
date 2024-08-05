package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.login.AuthLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.user.UserPaidTrack;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "用户管理接口")
@RestController
@RequestMapping("api/user/userInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserInfoApiController {

    @Autowired
    private UserInfoService userInfoService;

    @Operation(summary = "根据用户id查询用户")
    @GetMapping("getUserInfoById/{id}")
    public Result<UserInfoVo> getUserInfoById(@PathVariable("id") Long id) {
        return Result.ok(this.userInfoService.getUserInfoById(id));
    }

    @AuthLogin
    @Operation(summary = "根据专辑id查询当前用户是否订购过该专辑")
    @GetMapping("getPaidAlbumStat/{albumId}")
    public Result<Boolean> getPaidAlbumStat(@PathVariable("albumId") Long albumId) {
        return Result.ok(this.userInfoService.getPaidAlbumStat(albumId, AuthContextHolder.getUserId()));
    }

    @AuthLogin
    @Operation(summary = "根据专辑id查询当前用户购买过该专辑下的声音列表")
    @GetMapping("getPaidTracks/{albumId}")
    public Result<List<UserPaidTrack>> getPaidTracksByAlbumIdAndUserId(@PathVariable("albumId") Long albumId) {
        return Result.ok(this.userInfoService.getPaidTracksByAlbumIdAndUserId(albumId, AuthContextHolder.getUserId()));
    }
}

