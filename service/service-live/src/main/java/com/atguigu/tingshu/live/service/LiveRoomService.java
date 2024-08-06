package com.atguigu.tingshu.live.service;

import com.atguigu.tingshu.model.live.LiveRoom;
import com.atguigu.tingshu.vo.live.LiveRoomVo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface LiveRoomService extends IService<LiveRoom> {
    /**
     * 保存用户当前直播信息
     *
     * @param liveRoomVo 直播信息
     * @return LiveRoom
     */
    LiveRoom saveLiveRoom(LiveRoomVo liveRoomVo);
}
