package com.atguigu.tingshu.album.receiver;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.mapper.TrackStatMapper;
import com.atguigu.tingshu.common.constant.RabbitMqConstant;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.model.album.TrackStat;
import com.atguigu.tingshu.vo.album.StatMqVo;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.rabbitmq.client.Channel;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
public class TrackReceiver {
    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private TrackStatMapper trackStatMapper;

    @Resource
    private AlbumStatMapper albumStatMapper;

    /**
     * 更新 专辑或声音 的统计信息
     */
    @SneakyThrows
    @RabbitListener(
            bindings = @QueueBinding(
                    exchange = @Exchange(value = RabbitMqConstant.EXCHANGE_STAT_UPDATE, durable = "true"),
                    value = @Queue(value = RabbitMqConstant.QUEUE_STAT_UPDATE, durable = "true"),
                    key = RabbitMqConstant.ROUTING_STAT_UPDATE
            )
    )
    public void updateTrackStat(String statMqVoStr, Message message, Channel channel) {
        if (StringUtils.isBlank(statMqVoStr)) {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }
        // 反序列化为vo对象
        StatMqVo statMqVo = JSONObject.parseObject(statMqVoStr, StatMqVo.class);
        // 业务去重
        String businessNo = statMqVo.getBusinessNo();
        String key = "track:unique:" + businessNo;
        // 如果不存在则设置，否则返回false
        Boolean flag = this.redisTemplate.opsForValue().setIfAbsent(key, businessNo, 1, TimeUnit.HOURS);
        if (!flag) {
            // 重复消费
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }
        try {
            // 更新声音统计信息
            if (statMqVo.getTrackId() != null && StringUtils.isNotBlank(statMqVo.getTrackStatType())) {
                TrackStat trackStat = this.trackStatMapper.selectOne(Wrappers.lambdaQuery(TrackStat.class)
                        .eq(TrackStat::getTrackId, statMqVo.getTrackId())
                        .eq(TrackStat::getStatType, statMqVo.getTrackStatType()));
                trackStat.setStatNum(trackStat.getStatNum() + statMqVo.getCount());
                trackStat.setUpdateTime(new Date());
                this.trackStatMapper.updateById(trackStat);
            }
            // 更新专辑统计信息
            if (statMqVo.getAlbumId() != null && StringUtils.isNotBlank(statMqVo.getAlbumStatType())) {
                AlbumStat albumStat = this.albumStatMapper.selectOne(Wrappers.lambdaQuery(AlbumStat.class)
                        .eq(AlbumStat::getAlbumId, statMqVo.getAlbumId())
                        .eq(AlbumStat::getStatType, statMqVo.getAlbumStatType()));
                albumStat.setStatNum(albumStat.getStatNum() + statMqVo.getCount());
                albumStat.setUpdateTime(new Date());
                this.albumStatMapper.updateById(albumStat);
            }
            // 手动应答
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            // 如果出现异常，则删除key，可以进行重试
            this.redisTemplate.delete(key);
            if (!message.getMessageProperties().getRedelivered()) {
                // 消息已重复处理,拒绝再次接收
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            } else {
                // 该消息不是重复的
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            }
        }

    }
}
