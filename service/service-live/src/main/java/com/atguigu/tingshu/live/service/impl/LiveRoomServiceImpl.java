package com.atguigu.tingshu.live.service.impl;

import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.live.mapper.LiveRoomMapper;
import com.atguigu.tingshu.live.service.LiveRoomService;
import com.atguigu.tingshu.live.util.LiveAddressGenerator;
import com.atguigu.tingshu.model.live.LiveRoom;
import com.atguigu.tingshu.vo.live.LiveRoomVo;
import com.atguigu.tingshu.vo.live.TencentLiveAddressVo;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class LiveRoomServiceImpl extends ServiceImpl<LiveRoomMapper, LiveRoom> implements LiveRoomService {

    @Autowired
    private LiveRoomMapper liveRoomMapper;

    @Resource
    private LiveAddressGenerator liveAddressGenerator;

    @Override
    public LiveRoom saveLiveRoom(LiveRoomVo liveRoomVo) {
        Long userId = AuthContextHolder.getUserId();
        LiveRoom liveRoom = this.liveRoomMapper.selectOne(Wrappers.lambdaQuery(LiveRoom.class)
                .eq(LiveRoom::getUserId, userId)
                .gt(LiveRoom::getExpireTime, new Date()));
        // 如果存在未过期直播，则抛出异常
        if (!Objects.isNull(liveRoom)) {
            throw new GuiguException(ResultCodeEnum.EXIST_NO_EXPIRE_LIVE);
        }
        liveRoom = new LiveRoom();
        BeanUtils.copyProperties(liveRoomVo, liveRoom);
        liveRoom.setUserId(userId);
        liveRoom.setStatus("1");
        liveRoom.setAppName(liveAddressGenerator.getAppName());
        liveRoom.setStreamName("TingShu-" + userId);
        // 获取直播过期时间 单位：秒
        long expire = liveRoomVo.getExpireTime().getTime() / 1000;
        TencentLiveAddressVo addressUrl = this.liveAddressGenerator.getAddressUrl(liveRoom.getStreamName(), expire);
        liveRoom.setPushUrl(addressUrl.getPushWebRtcUrl());
        liveRoom.setPlayUrl(addressUrl.getPullWebRtcUrl());
        this.liveRoomMapper.insert(liveRoom);
        return liveRoom;
    }
}
