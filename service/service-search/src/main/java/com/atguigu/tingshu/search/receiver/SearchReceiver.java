package com.atguigu.tingshu.search.receiver;

import com.atguigu.tingshu.common.constant.RabbitMqConstant;
import com.atguigu.tingshu.search.service.SearchService;
import com.rabbitmq.client.Channel;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

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
    public void albumUpper(Long albumId, Message message, Channel channel) {
        if (!Objects.isNull(albumId)) {
            this.searchService.upperAlbum(albumId);
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
    public void albumLower(Long albumId, Message message, Channel channel) {
        if (!Objects.isNull(albumId)) {
            this.searchService.downAlbum(albumId);
        }
        // 手动应答
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
