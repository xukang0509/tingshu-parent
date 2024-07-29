package com.atguigu.tingshu.user.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.atguigu.tingshu.account.client.UserAccountFeignClient;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.user.mapper.UserInfoMapper;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Resource
    private UserInfoMapper userInfoMapper;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private WxMaService wxMaService;

    @Resource
    private UserAccountFeignClient userAccountFeignClient;

    @Override
    public Map<String, String> wxLogin(String code) {
        try {
            //  获取sessionInfo，其中包括：openid
            WxMaJscode2SessionResult sessionInfo = this.wxMaService.getUserService().getSessionInfo(code);
            String openId = null;
            if (!Objects.isNull(sessionInfo)) {
                openId = sessionInfo.getOpenid();
            }
            // 如果 openId 依然为空，抛异常
            if (Objects.isNull(openId)) {
                throw new GuiguException(ResultCodeEnum.ACCOUNT_ERROR);
            }
            // 根据 openId 获取用户信息
            UserInfo userInfo = this.userInfoMapper.selectOne(Wrappers.lambdaQuery(UserInfo.class)
                    .eq(UserInfo::getWxOpenId, openId));
            // 如果数据库中没有该用户，则新建一个新的用户
            if (Objects.isNull(userInfo)) {
                userInfo = new UserInfo();
                userInfo.setWxOpenId(openId);
                userInfo.setNickname("听友" + System.currentTimeMillis());
                userInfo.setAvatarUrl("http://192.168.10.103:9000/sph/6c0aa1b2fc2f49bda7107b1341102823.jpg");
                userInfo.setStatus("1");
                this.userInfoMapper.insert(userInfo);
                // 初始化账号信息
                userAccountFeignClient.saveUserAccount(userInfo.getId());
            }
            // 创建token
            String token = UUID.randomUUID().toString().replaceAll("-", "");
            // 将token及用户信息保存至redis中(token作为key，用户信息作为value)
            UserInfoVo userInfoVo = new UserInfoVo();
            BeanUtils.copyProperties(userInfo, userInfoVo);
            this.redisTemplate.opsForValue().set(RedisConstant.USER_LOGIN_KEY_PREFIX + token, userInfoVo,
                    RedisConstant.USER_LOGIN_KEY_TIMEOUT, TimeUnit.SECONDS);
            // 将token返回给前端
            Map<String, String> map = new HashMap<>();
            map.put("token", token);
            return map;
        } catch (WxErrorException e) {
            throw new RuntimeException(e);
        }
    }
}
