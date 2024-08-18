package com.atguigu.tingshu.user.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.client.TrackInfoFeignClient;
import com.atguigu.tingshu.common.constant.RabbitMqConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.service.RabbitService;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.user.*;
import com.atguigu.tingshu.user.login.LoginClient;
import com.atguigu.tingshu.user.login.LoginForm;
import com.atguigu.tingshu.user.mapper.*;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.album.StatMqVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.seata.spring.annotation.GlobalTransactional;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    @Resource
    private UserVipServiceMapper userVipServiceMapper;

    @Resource
    private VipServiceConfigMapper vipServiceConfigMapper;

    @Resource
    private TrackInfoFeignClient trackInfoFeignClient;

    @Resource
    private RabbitService rabbitService;


    @GlobalTransactional
    @Override
    public Map<String, Object> login(String code) {
        LoginForm loginForm = new LoginForm();
        loginForm.setCode(code);
        return loginClient.execute("1", loginForm);
    }

    //@TingShuCache(prefix = "user:info:", lockPrefix = "user:lock:")
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

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateUserPayRecord(UserPaidRecordVo userPaidRecordVo) {
        Long userId = userPaidRecordVo.getUserId();
        String orderNo = userPaidRecordVo.getOrderNo();
        List<Long> itemIdList = userPaidRecordVo.getItemIdList();
        String itemType = userPaidRecordVo.getItemType();
        if (userId == null || StringUtils.isEmpty(orderNo) ||
                StringUtils.isEmpty(itemType) || CollectionUtils.isEmpty(itemIdList)) {
            return;
        }

        if (SystemConstant.ORDER_ITEM_TYPE_VIP.equals(itemType)) {
            // 更新用户购买vip消费记录
            this.updateUserVipRecord(userPaidRecordVo);
        } else if (SystemConstant.ORDER_ITEM_TYPE_ALBUM.equals(itemType)) {
            // 更新用户购买专辑消费记录
            this.updateUserAlbumRecord(userPaidRecordVo);
        } else if (SystemConstant.ORDER_ITEM_TYPE_TRACK.equals(itemType)) {
            // 更新用户购买声音消费记录
            this.updateUserTrackRecord(userPaidRecordVo);
        } else {
            throw new RuntimeException("没有该交易类型！");
        }
    }

    private void updateUserTrackRecord(UserPaidRecordVo userPaidRecordVo) {
        Long userId = userPaidRecordVo.getUserId();
        String orderNo = userPaidRecordVo.getOrderNo();
        List<Long> itemIdList = userPaidRecordVo.getItemIdList();

        // 防止重复消费
        Long count = this.userPaidTrackMapper.selectCount(Wrappers.lambdaQuery(UserPaidTrack.class)
                .eq(UserPaidTrack::getUserId, userId)
                .eq(UserPaidTrack::getOrderNo, orderNo));
        if (count > 0) return;
        // 声音集合id为空，则直接返回
        if (CollectionUtils.isEmpty(itemIdList)) return;

        // 现根据声音id获取声音
        Result<TrackInfo> trackInfoRes = this.trackInfoFeignClient.getTrackInfo(itemIdList.get(0));
        Assert.notNull(trackInfoRes, "远程获取声音接口失败！");
        TrackInfo trackInfo = trackInfoRes.getData();
        Assert.notNull(trackInfo, "声音信息为空！");
        // 遍历购买的每一个声音id
        for (Long trackId : itemIdList) {
            UserPaidTrack userPaidTrack = new UserPaidTrack();
            userPaidTrack.setTrackId(trackId);
            userPaidTrack.setUserId(userId);
            userPaidTrack.setOrderNo(orderNo);
            userPaidTrack.setAlbumId(trackInfo.getAlbumId());
            this.userPaidTrackMapper.insert(userPaidTrack);
        }

        // 发送消息异步更新声音所在专辑的购买量
        StatMqVo statMqVo = new StatMqVo();
        // 防止重复消费的唯一标识
        statMqVo.setBusinessNo(UUID.randomUUID().toString().replace("-", ""));
        statMqVo.setAlbumId(trackInfo.getAlbumId());
        statMqVo.setAlbumStatType(SystemConstant.ALBUM_STAT_BROWSE);
        statMqVo.setCount(1);
        // 发送消息
        rabbitService.sendMessage(RabbitMqConstant.EXCHANGE_STAT_UPDATE,
                RabbitMqConstant.ROUTING_STAT_UPDATE, JSONObject.toJSONString(statMqVo));
    }

    private void updateUserAlbumRecord(UserPaidRecordVo userPaidRecordVo) {
        Long userId = userPaidRecordVo.getUserId();
        String orderNo = userPaidRecordVo.getOrderNo();
        List<Long> itemIdList = userPaidRecordVo.getItemIdList();

        // 防止重复消费
        Long count = this.userPaidAlbumMapper.selectCount(Wrappers.lambdaQuery(UserPaidAlbum.class)
                .eq(UserPaidAlbum::getOrderNo, orderNo)
                .eq(UserPaidAlbum::getUserId, userId));
        if (count > 0) {
            // TODO：发送消息走退款流程并把订单设置为无效订单
            return;
        }

        // 新增用户专辑购买记录
        UserPaidAlbum userPaidAlbum = new UserPaidAlbum();
        userPaidAlbum.setUserId(userId);
        userPaidAlbum.setOrderNo(orderNo);
        userPaidAlbum.setAlbumId(itemIdList.get(0));
        this.userPaidAlbumMapper.insert(userPaidAlbum);

        // 发送消息异步更新专辑的购买量
        StatMqVo statMqVo = new StatMqVo();
        // 防止重复消费的唯一标识
        statMqVo.setBusinessNo(UUID.randomUUID().toString().replace("-", ""));
        statMqVo.setAlbumId(userPaidAlbum.getAlbumId());
        statMqVo.setAlbumStatType(SystemConstant.ALBUM_STAT_BROWSE);
        statMqVo.setCount(1);
        // 发送消息
        rabbitService.sendMessage(RabbitMqConstant.EXCHANGE_STAT_UPDATE,
                RabbitMqConstant.ROUTING_STAT_UPDATE, JSONObject.toJSONString(statMqVo));
    }

    private void updateUserVipRecord(UserPaidRecordVo userPaidRecordVo) {
        Long userId = userPaidRecordVo.getUserId();
        String orderNo = userPaidRecordVo.getOrderNo();
        List<Long> itemIdList = userPaidRecordVo.getItemIdList();

        // 防止重复消费
        Long count = this.userVipServiceMapper.selectCount(Wrappers.lambdaQuery(UserVipService.class)
                .eq(UserVipService::getOrderNo, orderNo)
                .eq(UserVipService::getUserId, userId));
        if (count > 0) return;
        // 获取用户信息，计算开始生效时间和过期时间
        UserInfo userInfo = this.userInfoMapper.selectById(userId);
        Date startTime = new Date();
        if (userInfo.getIsVip() == 1 && userInfo.getVipExpireTime().after(startTime)) {
            startTime = userInfo.getVipExpireTime();
        }
        // 查询vip套餐，计算过期时间
        VipServiceConfig vipServiceConfig = this.vipServiceConfigMapper.selectById(itemIdList.get(0));
        Date expireTime = new DateTime(startTime).plusMonths(vipServiceConfig.getServiceMonth()).toDate();
        // 新增用户VIP购买记录
        UserVipService userVipService = new UserVipService();
        userVipService.setOrderNo(orderNo);
        userVipService.setUserId(userId);
        userVipService.setStartTime(startTime);
        userVipService.setExpireTime(expireTime);
        this.userVipServiceMapper.insert(userVipService);

        // 更新用户vip状态
        userInfo.setIsVip(1);
        userInfo.setVipExpireTime(expireTime);
        this.userInfoMapper.updateById(userInfo);
    }
}
