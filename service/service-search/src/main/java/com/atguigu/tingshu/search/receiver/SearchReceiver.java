package com.atguigu.tingshu.search.receiver;

import com.atguigu.tingshu.common.constant.RabbitMqConstant;
import com.atguigu.tingshu.search.service.SearchService;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.rabbitmq.client.Channel;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class SearchReceiver {
    @Resource
    private SearchService searchService;

    /**
     * 专辑上架
     */
    @SneakyThrows
    @RabbitListener(
            bindings = @QueueBinding(
                    exchange = @Exchange(value = RabbitMqConstant.EXCHANGE_ALBUM_UPPER, durable = "true"),
                    value = @Queue(value = RabbitMqConstant.QUEUE_ALBUM_UPPER, durable = "true"),
                    key = RabbitMqConstant.ROUTING_ALBUM_UPPER
            )
    )
    public void albumUpper(String albumId, Message message, Channel channel) {
        if (StringUtils.isNotBlank(albumId)) {
            this.searchService.upperAlbum(Long.parseLong(albumId));
        }
        // 手动应答
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    /**
     * 专辑下架
     */
    @SneakyThrows
    @RabbitListener(
            bindings = @QueueBinding(
                    exchange = @Exchange(value = RabbitMqConstant.EXCHANGE_ALBUM_LOWER, durable = "true"),
                    value = @Queue(value = RabbitMqConstant.QUEUE_ALBUM_LOWER, durable = "true"),
                    key = RabbitMqConstant.ROUTING_ALBUM_LOWER
            )
    )
    public void albumLower(String albumId, Message message, Channel channel) {
        if (StringUtils.isNotBlank(albumId)) {
            this.searchService.downAlbum(Long.parseLong(albumId));
        }
        // 手动应答
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
