package com.atguigu.tingshu.album.receiver;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.pojo.MaxWellObj;
import com.atguigu.tingshu.common.constant.RabbitMqConstant;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.service.RabbitService;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.rabbitmq.client.Channel;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author xk
 * @since 2024-08-12 18:28
 */
@Component
public class AlbumInfoCacheReceiver {

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private RabbitService rabbitService;

    @SneakyThrows
    @RabbitListener(queues = {"tingshu.album.cache"})
    public void albumInfoCacheManager(String json, Message message, Channel channel) {
        if (StringUtils.isBlank(json)) {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }
        MaxWellObj maxWellObj = JSONObject.parseObject(json, MaxWellObj.class);
        // 处理 albumInfo 的缓存处理
        if ("tingshu_album".equals(maxWellObj.getDatabase()) && "album_info".equals(maxWellObj.getTable())) {
            processAlbumCache(maxWellObj);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }
    }

    private void processAlbumCache(MaxWellObj maxWellObj) {
        AlbumInfo data = JSONObject.parseObject(maxWellObj.getData(), AlbumInfo.class);
        String key = RedisConstant.ALBUM_INFO_PREFIX + data.getId();
        if ("delete".equals(maxWellObj.getType())) {
            rabbitService.sendMessage(RabbitMqConstant.EXCHANGE_ALBUM_LOWER,
                    RabbitMqConstant.ROUTING_ALBUM_LOWER, data.getId());
        } else {
            if ("1".equals(data.getIsOpen())) {
                rabbitService.sendMessage(RabbitMqConstant.EXCHANGE_ALBUM_UPPER,
                        RabbitMqConstant.ROUTING_ALBUM_UPPER, data.getId());
            } else {
                rabbitService.sendMessage(RabbitMqConstant.EXCHANGE_ALBUM_LOWER,
                        RabbitMqConstant.ROUTING_ALBUM_LOWER, data.getId());
            }
        }
        if (!"update".equals(maxWellObj.getType())) return;
        if (data.getIsDeleted() == 1) {
            // 逻辑删除
            this.redisTemplate.delete(key);
        } else {
            // 修改，先看缓存中是否有该数据，如果有则更新该数据
            AlbumInfo old = (AlbumInfo) this.redisTemplate.opsForValue().get(key);
            if (old == null) return;
            this.redisTemplate.opsForValue().set(key, data, RedisConstant.ALBUM_TIMEOUT, TimeUnit.SECONDS);
        }
    }
}
