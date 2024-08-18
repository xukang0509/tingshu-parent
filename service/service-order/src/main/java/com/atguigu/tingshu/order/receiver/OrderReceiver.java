package com.atguigu.tingshu.order.receiver;

import com.atguigu.tingshu.common.constant.RabbitMqConstant;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.rabbitmq.client.Channel;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * @author xk
 * @since 2024-08-16 15:01
 */
@Component
@Slf4j
public class OrderReceiver {
    @Resource
    private OrderInfoService orderInfoService;

    /**
     * 支付成功 更新订单状态
     *
     * @param orderNo
     * @param message
     * @param channel
     */
    @SneakyThrows
    @RabbitListener(
            bindings = @QueueBinding(
                    exchange = @Exchange(value = RabbitMqConstant.EXCHANGE_ORDER_PAY_SUCCESS, durable = "true"),
                    value = @Queue(value = RabbitMqConstant.QUEUE_ORDER_PAY_SUCCESS, durable = "true"),
                    key = RabbitMqConstant.ROUTING_ORDER_PAY_SUCCESS
            )
    )
    public void paySuccess(String orderNo, Message message, Channel channel) {
        if (StringUtils.isBlank(orderNo)) {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }
        try {
            this.orderInfoService.updateOrderStatus(orderNo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            if (!message.getMessageProperties().getRedelivered()) {
                // 消息已重复处理,拒绝再次接收
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            } else {
                // 该消息不是重复的
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            }
        }
    }

    /**
     * 定时取消订单
     */
    @SneakyThrows
    @RabbitListener(queues = RabbitMqConstant.QUEUE_CANCEL_ORDER)
    public void cancelOrder(String orderNo, Message message, Channel channel) {
        if (StringUtils.isBlank(orderNo)) {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }
        try {
            this.orderInfoService.cancelOrder(orderNo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
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
