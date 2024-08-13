package com.atguigu.tingshu.user.service.impl;

import com.atguigu.tingshu.common.cache.TingShuCache;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.model.user.UserPaidAlbum;
import com.atguigu.tingshu.model.user.UserPaidTrack;
import com.atguigu.tingshu.user.login.LoginClient;
import com.atguigu.tingshu.user.login.LoginForm;
import com.atguigu.tingshu.user.mapper.UserInfoMapper;
import com.atguigu.tingshu.user.mapper.UserPaidAlbumMapper;
import com.atguigu.tingshu.user.mapper.UserPaidTrackMapper;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.seata.spring.annotation.GlobalTransactional;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.LocalDate;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Resource
    private UserInfoMapper userInfoMapper;

    @Resource
    private LoginClient loginClient;

    @Resource
    private UserPaidAlbumMapper userPaidAlbumMapper;

    @Resource
    private UserPaidTrackMapper userPaidTrackMapper;

    @GlobalTransactional
    @Override
    public Map<String, Object> login(String code) {
        LoginForm loginForm = new LoginForm();
        loginForm.setCode(code);
        return loginClient.execute("1", loginForm);
    }

    @TingShuCache(prefix = "user:info:", lockPrefix = "user:lock:")
    @Override
    public UserInfoVo getUserInfoById(Long id) {
        UserInfo userInfo = this.userInfoMapper.selectById(id);
        UserInfoVo userInfoVo = new UserInfoVo();
        BeanUtils.copyProperties(userInfo, userInfoVo);
        return userInfoVo;
    }

    @Override
    public Boolean getPaidAlbumStat(Long albumId, Long userId) {
        return this.userPaidAlbumMapper.selectCount(Wrappers.lambdaQuery(UserPaidAlbum.class)
                .eq(UserPaidAlbum::getAlbumId, albumId)
                .eq(UserPaidAlbum::getUserId, userId)) > 0;
    }

    @Override
    public List<UserPaidTrack> getPaidTracksByAlbumIdAndUserId(Long albumId, Long userId) {
        return this.userPaidTrackMapper.selectList(Wrappers.lambdaQuery(UserPaidTrack.class)
                .eq(UserPaidTrack::getAlbumId, albumId)
                .eq(UserPaidTrack::getUserId, userId));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateExpiredVipStatus() {
        // 获取今日凌晨时间：如果用户到今日凌晨时，vip已过期则更新为非vip
        Date now = LocalDate.now().toDate();
        UserInfo userInfo = new UserInfo();
        userInfo.setIsVip(0);
        this.userInfoMapper.update(userInfo, Wrappers.lambdaQuery(UserInfo.class)
                .eq(UserInfo::getIsVip, 1)
                .lt(UserInfo::getVipExpireTime, now));
    }
}
