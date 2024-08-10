package com.atguigu.tingshu.user.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.vo.user.UserListenProcessVo;

import java.math.BigDecimal;

public interface UserListenProcessService {

    /**
     * 根据声音id获取播放进度
     *
     * @param trackId 声音id
     * @return 播放进度
     */
    BigDecimal getTrackBreakSecond(Long trackId);

    /**
     * 更新播放进度
     */
    void updateListenProcess(UserListenProcessVo userListenProcessVo);

    /**
     * 获取最近一次播放声音
     *
     * @return 播放记录
     */
    JSONObject getLatelyTrack();
}
