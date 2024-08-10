package com.atguigu.tingshu.user.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.common.constant.RabbitMqConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.service.RabbitService;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.common.util.MongoUtil;
import com.atguigu.tingshu.model.user.UserListenProcess;
import com.atguigu.tingshu.user.service.UserListenProcessService;
import com.atguigu.tingshu.vo.album.StatMqVo;
import com.atguigu.tingshu.vo.user.UserListenProcessVo;
import jakarta.annotation.Resource;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserListenProcessServiceImpl implements UserListenProcessService {
    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private RabbitService rabbitService;

    @Override
    public BigDecimal getTrackBreakSecond(Long trackId) {
        Long userId = AuthContextHolder.getUserId();
        if (userId == null) {
            // 如果用户未登录，则从头开始播放
            return new BigDecimal("0");
        }
        // 根据用户Id、声音Id获取播放进度对象
        UserListenProcess userListenProcess = getUserListenProcess(userId, trackId);
        if (userListenProcess == null) {
            // 如果该用户没有当前声音的播放进度，则从头开始播放。
            return new BigDecimal("0");
        }
        // 获取到播放的跳出时间
        return userListenProcess.getBreakSecond();
    }

    @Override
    public void updateListenProcess(UserListenProcessVo userListenProcessVo) {
        Long userId = AuthContextHolder.getUserId();
        Long trackId = userListenProcessVo.getTrackId();
        if (userId == null || trackId == null) return;
        // 根据用户Id、声音Id获取播放进度对象
        UserListenProcess userListenProcess = getUserListenProcess(userId, trackId);
        if (userListenProcess != null) {
            // mongoDb中更新播放进度
            userListenProcess.setBreakSecond(userListenProcessVo.getBreakSecond());
            userListenProcess.setUpdateTime(new Date());
        } else {
            // mongoDb中初始化播放进度对象，保存播放进度
            userListenProcess = new UserListenProcess();
            userListenProcess.setId(ObjectId.get().toString());
            userListenProcess.setUserId(userId);
            userListenProcess.setTrackId(trackId);
            userListenProcess.setAlbumId(userListenProcessVo.getAlbumId());
            userListenProcess.setBreakSecond(userListenProcessVo.getBreakSecond());
            userListenProcess.setIsShow(1);
            userListenProcess.setCreateTime(new Date());
            userListenProcess.setUpdateTime(userListenProcess.getCreateTime());
        }
        this.mongoTemplate.save(userListenProcess, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));

        // 判断是否已经统计过：同一个用户同一个声音每天只记录一次播放量
        String key = "user:track:" + new DateTime().toString("yyyy-MM-dd") + ":" + userId;
        Boolean isExist = this.redisTemplate.opsForValue().getBit(key, trackId);
        // 如果不存在，则记录为已统计并发送消息异步统计
        if (!isExist) {
            // 标记为已统计
            this.redisTemplate.opsForValue().setBit(key, trackId, true);
            // 设置过期时间：只需要记录到凌晨即可，这里计算过期时间 = 明天凌晨 - 当前时间
            // 明日凌晨
            LocalDateTime nextDay = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIN);
            // 计算过期时间
            long expireTime = ChronoUnit.SECONDS.between(LocalDateTime.now(), nextDay);
            this.redisTemplate.expire(key, expireTime, TimeUnit.SECONDS);

            // 组装消息
            StatMqVo trackStatMqVo = new StatMqVo();
            // 防止重复消费的唯一标识
            trackStatMqVo.setBusinessNo(UUID.randomUUID().toString().replace("-", ""));
            trackStatMqVo.setAlbumId(userListenProcessVo.getAlbumId());
            trackStatMqVo.setTrackId(trackId);
            trackStatMqVo.setTrackStatType(SystemConstant.TRACK_STAT_PLAY);
            trackStatMqVo.setAlbumStatType(SystemConstant.ALBUM_STAT_PLAY);
            trackStatMqVo.setCount(1);
            // 发送消息
            rabbitService.sendMessage(RabbitMqConstant.EXCHANGE_STAT_UPDATE,
                    RabbitMqConstant.ROUTING_STAT_UPDATE, JSONObject.toJSONString(trackStatMqVo));
        }
    }

    @Override
    public JSONObject getLatelyTrack() {
        JSONObject data = new JSONObject();
        // 获取用户id
        Long userId = AuthContextHolder.getUserId();
        // 根据用户id查询播放进度
        Query query = Query.query(Criteria.where("userId").is(userId));
        // 查询最近的播放进度
        Sort sort = Sort.by(Sort.Order.desc("updateTime"));
        UserListenProcess userListenProcess = this.mongoTemplate.findOne(query.with(sort), UserListenProcess.class,
                MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
        if (userListenProcess == null) return data;
        data.put("trackId", userListenProcess.getTrackId());
        data.put("albumId", userListenProcess.getAlbumId());
        return data;
    }

    private UserListenProcess getUserListenProcess(Long userId, Long trackId) {
        Query query = Query.query(Criteria.where("userId").is(userId).and("trackId").is(trackId));
        // 根据用户id计算该用户的播放进度应该存放的集合(Collection)
        String collectionName = MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId);
        // 执行查询
        return mongoTemplate.findOne(query, UserListenProcess.class, collectionName);
    }
}
